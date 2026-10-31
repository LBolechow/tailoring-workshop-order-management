package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.BookingSlotService;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSlotServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrderRepository orderRepository;
    @InjectMocks BookingSlotService bookingSlotService;

    private final Date start = Date.from(Instant.parse("2026-11-02T09:00:00Z"));
    private final Date end = Date.from(Instant.parse("2026-11-02T10:00:00Z"));

    @BeforeEach
    void setup() {
        // Each test explicitly specifies repository responses it uses.
    }

    @Test
    void addOrderMustLockEmployeeBeforeCheckingOverlap() {
        User employee = new User();
        employee.setId(1L);
        when(userRepository.findByIdForScheduling(1L)).thenReturn(Optional.of(employee));
        when(orderRepository.existsOverlappingSlot(1L, start, end)).thenReturn(false);

        assertThatCode(() -> bookingSlotService.lockAndCheckSlot(1L, start, end, null))
                .doesNotThrowAnyException();

        var inOrder = inOrder(userRepository, orderRepository);
        inOrder.verify(userRepository).findByIdForScheduling(1L);
        inOrder.verify(orderRepository).existsOverlappingSlot(1L, start, end);
    }

    @Test
    void overlappingSlotIsRejectedBeforeInsert() {
        User employee = new User();
        employee.setId(1L);
        when(userRepository.findByIdForScheduling(1L)).thenReturn(Optional.of(employee));
        when(orderRepository.existsOverlappingSlot(1L, start, end)).thenReturn(true);

        assertThatThrownBy(() -> bookingSlotService.lockAndCheckSlot(1L, start, end, null))
                .isInstanceOf(ApplicationException.OrderSlotConflictException.class);
    }

    @Test
    void editIgnoresOnlyCurrentOrder() {
        User employee = new User();
        employee.setId(1L);
        when(userRepository.findByIdForScheduling(1L)).thenReturn(Optional.of(employee));
        when(orderRepository.existsOverlappingSlotExcludingOrder(1L, start, end, 77L))
                .thenReturn(false);

        assertThatCode(() -> bookingSlotService.lockAndCheckSlot(1L, start, end, 77L))
                .doesNotThrowAnyException();
        verify(orderRepository, never()).existsOverlappingSlot(1L, start, end);
    }

    @Test
    void invalidIntervalDoesNotLockAnything() {
        assertThatThrownBy(() -> bookingSlotService.lockAndCheckSlot(1L, end, start, null))
                .isInstanceOf(ApplicationException.InvalidDateException.class);
        verifyNoInteractions(userRepository, orderRepository);
    }
}
