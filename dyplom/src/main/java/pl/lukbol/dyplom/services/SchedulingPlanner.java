package pl.lukbol.dyplom.services;

import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.DTOs.order.AvailabilityDTO;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.utilities.DateUtils;

import java.util.*;
import java.util.function.BiFunction;

/** Pure scheduling search: no repositories, no writes, deterministic for a given clock input. */
@Component
public class SchedulingPlanner {
    public boolean isWorkingDay(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        return day != Calendar.SATURDAY && day != Calendar.SUNDAY;
    }

    /**
     * Finds the first available slot using half-hour boundaries during working hours.
     * A busy slot is followed by another candidate on the same day, not the next day.
     */
    public AvailabilityDTO findNextAvailableSlot(
            Date startDate,
            double durationHours,
            BiFunction<Calendar, Calendar, List<User>> availableUsersProvider) {

        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(availableUsersProvider, "availableUsersProvider");
        if (!Double.isFinite(durationHours) || durationHours <= 0) {
            throw new IllegalArgumentException(Messages.INVALID_DURATION);
        }

        final int WORKDAY_END_HOUR = 16;
        final int START_HOUR = 8;
        final int SLOT_STEP_MINUTES = 30;
        final int MAX_DAYS_LOOKAHEAD = 30;
        final int durationMinutes = (int) Math.round(durationHours * 60);

        Calendar currentDateTime = Calendar.getInstance(TimeZone.getTimeZone(DateUtils.WARSAW_ZONE));
        currentDateTime.setTime(startDate);
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone(DateUtils.WARSAW_ZONE));
        if (currentDateTime.before(now)) {
            currentDateTime = now;
        }
        roundUpToSlotBoundary(currentDateTime, SLOT_STEP_MINUTES);

        int daysChecked = 0;
        while (daysChecked < MAX_DAYS_LOOKAHEAD) {
            if (!isWorkingDay(currentDateTime)) {
                advanceToNextDayStart(currentDateTime, START_HOUR);
                daysChecked++;
                continue;
            }

            if (currentDateTime.get(Calendar.HOUR_OF_DAY) < START_HOUR) {
                currentDateTime.set(Calendar.HOUR_OF_DAY, START_HOUR);
                currentDateTime.set(Calendar.MINUTE, 0);
            }

            Calendar endDateTime = (Calendar) currentDateTime.clone();
            endDateTime.add(Calendar.MINUTE, durationMinutes);

            Calendar workdayEnd = (Calendar) currentDateTime.clone();
            workdayEnd.set(Calendar.HOUR_OF_DAY, WORKDAY_END_HOUR);
            workdayEnd.set(Calendar.MINUTE, 0);
            workdayEnd.set(Calendar.SECOND, 0);
            workdayEnd.set(Calendar.MILLISECOND, 0);

            if (endDateTime.after(workdayEnd)) {
                advanceToNextDayStart(currentDateTime, START_HOUR);
                daysChecked++;
                continue;
            }

            List<User> availableUsers = availableUsersProvider.apply(currentDateTime, endDateTime);
            if (!availableUsers.isEmpty()) {
                return new AvailabilityDTO(
                        true,
                        Messages.SLOT_AVAILABLE,
                        DateUtils.formatDateTime(currentDateTime),
                        DateUtils.formatDateTime(endDateTime),
                        availableUsers.get(0).getName()
                );
            }

            currentDateTime.add(Calendar.MINUTE, SLOT_STEP_MINUTES);
        }

        return new AvailabilityDTO(
                false,
                Messages.NO_AVAILABLE_EMPLOYEES,
                null, null, null
        );
    }

    private void roundUpToSlotBoundary(Calendar calendar, int stepMinutes) {
        boolean hasPartialMinute = calendar.get(Calendar.SECOND) != 0
                || calendar.get(Calendar.MILLISECOND) != 0;
        int remainder = calendar.get(Calendar.MINUTE) % stepMinutes;
        int minutesToAdd = remainder == 0
                ? (hasPartialMinute ? stepMinutes : 0)
                : stepMinutes - remainder;
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, minutesToAdd);
    }

    private void advanceToNextDayStart(Calendar calendar, int startHour) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

}
