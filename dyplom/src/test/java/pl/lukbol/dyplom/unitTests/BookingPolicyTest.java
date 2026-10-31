package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.services.BookingPolicy;
import pl.lukbol.dyplom.utilities.DateUtils;

import static org.assertj.core.api.Assertions.*;

class BookingPolicyTest {
    private final BookingPolicy policy = new BookingPolicy();

    private java.util.Date at(String time) { return DateUtils.parseOrderDateTime(time); }

    @Test void weekdayWithinHoursIsAccepted() {
        assertThatCode(() -> policy.validateDuration(at("2026-11-02T09:30:00"),
                at("2026-11-02T11:00:00"), 1.5)).doesNotThrowAnyException();
    }
    @Test void weekendIsRejected() {
        assertThatThrownBy(() -> policy.validate(at("2026-11-07T09:00:00"),
                at("2026-11-07T10:00:00")))
                .isInstanceOf(ApplicationException.InvalidDateException.class);
    }
    @Test void outsideWorkingHoursIsRejected() {
        assertThatThrownBy(() -> policy.validate(at("2026-11-02T15:00:00"),
                at("2026-11-02T17:00:00")))
                .isInstanceOf(ApplicationException.InvalidDateException.class);
    }
    @Test void inconsistentDurationIsRejected() {
        assertThatThrownBy(() -> policy.validateDuration(at("2026-11-02T09:00:00"),
                at("2026-11-02T10:00:00"), 2.0))
                .isInstanceOf(ApplicationException.InvalidDateException.class);
    }
    @Test void arbitraryStartMinuteIsRejected() {
        assertThatThrownBy(() -> policy.validate(at("2026-11-02T09:15:00"),
                at("2026-11-02T10:15:00")))
                .isInstanceOf(ApplicationException.InvalidDateException.class);
    }
    @Test void adjacentSlotsAreValidIndividually() {
        assertThatCode(() -> policy.validateDuration(at("2026-11-02T09:00:00"),
                at("2026-11-02T10:00:00"), 1.0)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateDuration(at("2026-11-02T10:00:00"),
                at("2026-11-02T11:00:00"), 1.0)).doesNotThrowAnyException();
    }
}
