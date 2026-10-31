package pl.lukbol.dyplom.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.util.Date;

/**
 * Authoritative availability check for writes, not for calendar previews.
 *
 * The caller MUST be @Transactional. A pessimistic lock on the employee row
 * serializes insert/edit operations for that employee in PostgreSQL. The
 * overlap query runs AFTER acquiring the lock and before the transaction
 * commits. That makes concurrent requests for the same interval safe.
 */
@Service
@RequiredArgsConstructor
public class BookingSlotService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BookingPolicy bookingPolicy = new BookingPolicy();

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockAndCheckSlot(Long employeeId, Date start, Date end, Long excludedOrderId) {
        bookingPolicy.validate(start, end);

        // PostgreSQL holds this row lock until the surrounding transaction ends.
        userRepository.findByIdForScheduling(employeeId)
                .orElseThrow(() -> new ApplicationException.UserNotFoundException(
                        Messages.EMPLOYEE_NOT_FOUND + employeeId + ")"));

        boolean occupied = excludedOrderId == null
                ? orderRepository.existsOverlappingSlot(employeeId, start, end)
                : orderRepository.existsOverlappingSlotExcludingOrder(
                        employeeId, start, end, excludedOrderId);

        if (occupied) {
            throw new ApplicationException.OrderSlotConflictException(
                    Messages.ORDER_SLOT_CONFLICT);
        }
    }
}
