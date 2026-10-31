package pl.lukbol.dyplom.utilities;

import pl.lukbol.dyplom.DTOs.date.DateRange;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.common.Messages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public final class DateUtils {

    public static final ZoneId WARSAW_ZONE = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter OUTPUT_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private DateUtils() {
    }

    // Legacy helper retained for other callers and existing tests.
    public static Date parseDate(String dateString, String pattern) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        return dateFormat.parse(dateString);
    }

    /**
     * Order API accepts a Warsaw-local ISO datetime, e.g. 2026-11-02T09:30:00.
     * Date-only values remain supported for older callers, mapped to midnight.
     */
    public static Date parseOrderDateTime(String value) {
        if (value == null || value.isBlank()) {
            throw new ApplicationException.InvalidDateException(Messages.EMPTY_ORDER_DATE);
        }
        try {
            LocalDateTime localDateTime = value.length() == 10
                    ? LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay()
                    : LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Date.from(localDateTime.atZone(WARSAW_ZONE).toInstant());
        } catch (DateTimeException ex) {
            throw new ApplicationException.InvalidDateException(
                    Messages.INVALID_ORDER_DATE);
        }
    }

    public static DateRange parseOrderRange(String startValue, String endValue) {
        Date start = parseOrderDateTime(startValue);
        Date end = parseOrderDateTime(endValue);
        if (!end.after(start)) {
            throw new ApplicationException.InvalidDateException(
                    Messages.INVALID_ORDER_RANGE);
        }
        return new DateRange(start, end);
    }

    /** Search accepts either ISO local dates or ISO local datetimes in Warsaw. */
    public static DateRange parseSearchRange(String start, String end) {
        if (start == null || end == null) {
            java.time.LocalDate today = java.time.LocalDate.now(WARSAW_ZONE);
            return new DateRange(
                    Date.from(today.atStartOfDay(WARSAW_ZONE).toInstant()),
                    Date.from(today.plusDays(1).atStartOfDay(WARSAW_ZONE).toInstant().minusNanos(1)));
        }
        try {
            java.time.LocalDateTime a = start.length() == 10
                    ? java.time.LocalDate.parse(start).atStartOfDay()
                    : java.time.LocalDateTime.parse(start, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            java.time.LocalDateTime b = end.length() == 10
                    ? java.time.LocalDate.parse(end).plusDays(1).atStartOfDay().minusNanos(1)
                    : java.time.LocalDateTime.parse(end, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (b.isBefore(a)) throw new DateTimeException(Messages.INVALID_ORDER_RANGE);
            return new DateRange(Date.from(a.atZone(WARSAW_ZONE).toInstant()),
                                 Date.from(b.atZone(WARSAW_ZONE).toInstant()));
        } catch (DateTimeException ex) {
            throw new ApplicationException.InvalidDateException(Messages.INVALID_ORDER_DATE);
        }
    }

    public static String formatOrderDateTime(Date value) {
        return OUTPUT_DATE_TIME.format(LocalDateTime.ofInstant(value.toInstant(), WARSAW_ZONE));
    }

    /** Calendar previews use the same Warsaw zone as persisted orders. */
    public static String formatDateTime(Calendar calendar) {
        return formatOrderDateTime(calendar.getTime());
    }
}
