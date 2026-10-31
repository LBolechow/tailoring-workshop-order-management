package pl.lukbol.dyplom.services;

import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.utilities.DateUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

/** Server-side business rules, shared by inserts and edits. Preview alone is not authoritative. */
@Component
public class BookingPolicy {
    public void validate(Date start, Date end) {
        if (start == null || end == null || !end.after(start)) {
            throw new ApplicationException.InvalidDateException(Messages.INVALID_ORDER_RANGE);
        }
        LocalDateTime a = LocalDateTime.ofInstant(start.toInstant(), DateUtils.WARSAW_ZONE);
        LocalDateTime b = LocalDateTime.ofInstant(end.toInstant(), DateUtils.WARSAW_ZONE);
        if (!a.toLocalDate().equals(b.toLocalDate()) || a.getDayOfWeek() == DayOfWeek.SATURDAY
                || a.getDayOfWeek() == DayOfWeek.SUNDAY || a.toLocalTime().isBefore(java.time.LocalTime.of(8, 0))
                || b.toLocalTime().isAfter(java.time.LocalTime.of(16, 0))) {
            throw new ApplicationException.InvalidDateException(Messages.OUTSIDE_WORKING_HOURS);
        }
        if (a.getMinute() % 30 != 0 || a.getSecond() != 0 || a.getNano() != 0) {
            throw new ApplicationException.InvalidDateException(Messages.INVALID_BOOKING_START);
        }
    }

    public void validateDuration(Date start, Date end, double hours) {
        validate(start, end);
        long minutes = Duration.between(start.toInstant(), end.toInstant()).toMinutes();
        if (!Double.isFinite(hours) || hours < 0.5 || hours > 8.0
                || minutes != Math.round(hours * 60) || minutes % 30 != 0) {
            throw new ApplicationException.InvalidDateException(Messages.INVALID_BOOKING_DURATION);
        }
    }
}
