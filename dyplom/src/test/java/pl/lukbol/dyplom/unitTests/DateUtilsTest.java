package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import pl.lukbol.dyplom.utilities.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateUtilsTest {

    // parseDate

    @Test
    void parseDate_shouldParseIsoDate() throws ParseException {
        Date result = DateUtils.parseDate("2026-03-15", "yyyy-MM-dd");

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);

        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2026);
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(Calendar.MARCH);
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(15);
    }

    @Test
    void parseDate_shouldParseDateTimePattern() throws ParseException {
        Date result = DateUtils.parseDate("2026-03-15 14:30", "yyyy-MM-dd HH:mm");

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);

        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(14);
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(30);
    }

    @Test
    void parseDate_shouldThrowParseException_whenStringDoesNotMatchPattern() {
        assertThatThrownBy(() -> DateUtils.parseDate("nie-data", "yyyy-MM-dd"))
                .isInstanceOf(ParseException.class);
    }

    @Test
    void parseDate_shouldThrowParseException_whenStringIsEmpty() {
        assertThatThrownBy(() -> DateUtils.parseDate("", "yyyy-MM-dd"))
                .isInstanceOf(ParseException.class);
    }

    // formatDateTime

    @Test
    void formatDateTime_shouldReturnIsoLikeFormat() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MARCH, 15, 9, 5, 0);

        String result = DateUtils.formatDateTime(cal);

        assertThat(result).isEqualTo("2026-03-15T09:05:00");
    }

    @Test
    void formatDateTime_shouldPadSingleDigitValues() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 2, 3, 4, 5);

        assertThat(DateUtils.formatDateTime(cal)).isEqualTo("2026-01-02T03:04:05");
    }

    // parseDate + formatDateTime

    @Test
    void parseDateAndFormatDateTime_shouldRoundTripTheSameInstant() throws ParseException {
        Date parsed = DateUtils.parseDate("2026-07-01", "yyyy-MM-dd");

        Calendar cal = Calendar.getInstance();
        cal.setTime(parsed);

        assertThat(DateUtils.formatDateTime(cal))
                .startsWith(new SimpleDateFormat("yyyy-MM-dd").format(parsed));
    }
}
