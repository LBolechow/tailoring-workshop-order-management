package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import pl.lukbol.dyplom.utilities.DateUtils;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import static org.assertj.core.api.Assertions.*;

class DateUtilsWarsawTest {
    @Test void formatsDateInWarsawRegardlessOfJvmDefaultTimeZone() {
        var now = java.util.Date.from(java.time.Instant.parse("2026-11-02T08:00:00Z"));
        assertThat(DateUtils.formatOrderDateTime(now)).isEqualTo("2026-11-02T09:00:00");
    }
    @Test void dateOnlyRangeIncludesWholeDay() {
        var range = DateUtils.parseSearchRange("2026-11-02", "2026-11-02");
        assertThat(DateUtils.formatOrderDateTime(range.start())).startsWith("2026-11-02T00:00:00");
        assertThat(DateUtils.formatOrderDateTime(range.end())).startsWith("2026-11-02T23:59:59");
    }
    @Test void malformedRangeReturnsControlledException() {
        assertThatThrownBy(() -> DateUtils.parseSearchRange("invalid", "2026-11-02"))
                .isInstanceOf(ApplicationException.InvalidDateException.class);
    }
}
