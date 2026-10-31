package pl.lukbol.dyplom.utilities;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.DTOs.order.OrderDetailsDTO;
import pl.lukbol.dyplom.DTOs.order.BookingInterval;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.common.UserRole;
import pl.lukbol.dyplom.DTOs.material.MaterialDTO;
import pl.lukbol.dyplom.DTOs.date.DateRange;
import pl.lukbol.dyplom.DTOs.order.AvailabilityDTO;
import pl.lukbol.dyplom.DTOs.order.EditOrderDTO;
import pl.lukbol.dyplom.DTOs.order.OrderDTO;
import pl.lukbol.dyplom.classes.Material;
import pl.lukbol.dyplom.classes.Notification;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderUtils {

    public static final ZoneId WARSAW_ZONE = ZoneId.of("Europe/Warsaw");

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MaterialRepository materialRepository;

    public boolean isWorkingDay(Calendar calendar) {
        return schedulingPlanner.isWorkingDay(calendar);
    }

    private static final List<String> SCHEDULABLE_ROLES = List.of(
            UserRole.EMPLOYEE.authority(), UserRole.ADMIN.authority());

    /**
     * Build a read-only snapshot for a complete 30-day slot search.
     * Instead of fetching employees and their orders for every 30-minute
     * candidate, load eligible employees once and their intervals once.
     *
     * This is only a preview. Writes must still use BookingSlotService's
     * transactional lock-and-recheck to prevent stale preview races.
     */
    public BiFunction<Calendar, Calendar, List<User>> prepareAvailabilityLookup(
            Date requestedStart, Long onlyEmployeeId, Long excludedEmployeeId) {
        if (requestedStart == null) {
            throw new IllegalArgumentException(Messages.NULL_DATE_EXCEPTION);
        }
        Date now = new Date();
        Date from = requestedStart.after(now) ? requestedStart : now;
        Calendar horizonEnd = Calendar.getInstance(TimeZone.getTimeZone(WARSAW_ZONE));
        horizonEnd.setTime(from);
        // findNextAvailableSlot inspects at most 30 days; leave a margin for
        // rounding and weekends, including a DST boundary in Warsaw.
        horizonEnd.add(Calendar.DAY_OF_MONTH, 32);
        AvailabilitySnapshot snapshot = loadAvailabilitySnapshot(
                from, horizonEnd.getTime(), onlyEmployeeId, excludedEmployeeId);
        return (start, end) -> snapshot.available(start.getTime(), end.getTime());
    }

    /** Single-slot compatibility API; only two DB queries, not N+1. */
    public List<User> findAvailableUsersWithEndDateTime(Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        validateDates(taskStartDateTime, taskEndDateTime);
        return loadAvailabilitySnapshot(taskStartDateTime, taskEndDateTime, null, null)
                .available(taskStartDateTime, taskEndDateTime);
    }

    public List<User> findAvailableUserWithEndDateTime(Long employeeId, Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        validateDates(taskStartDateTime, taskEndDateTime);
        return loadAvailabilitySnapshot(taskStartDateTime, taskEndDateTime, employeeId, null)
                .available(taskStartDateTime, taskEndDateTime);
    }

    public List<User> findAvailableUsersWithoutEmployee(Long orderId, Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        validateDates(taskStartDateTime, taskEndDateTime);
        return orderRepository.findById(orderId)
                .map(order -> loadAvailabilitySnapshot(
                        taskStartDateTime, taskEndDateTime, null, order.getEmployee().getId())
                        .available(taskStartDateTime, taskEndDateTime))
                .orElseGet(List::of);
    }

    private void validateDates(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException(Messages.NULL_DATE_EXCEPTION);
        }
    }

    private AvailabilitySnapshot loadAvailabilitySnapshot(Date from, Date to,
                                                           Long onlyEmployeeId, Long excludedEmployeeId) {
        List<User> staff = userRepository.findUsersByRole_NameIn(SCHEDULABLE_ROLES).stream()
                .filter(user -> user.getId() != null && user.getRole() != null
                        && SCHEDULABLE_ROLES.contains(user.getRole().getName()))
                .filter(user -> onlyEmployeeId == null || onlyEmployeeId.equals(user.getId()))
                .filter(user -> excludedEmployeeId == null || !excludedEmployeeId.equals(user.getId()))
                .sorted(Comparator.comparing(User::getId))
                .toList();

        if (staff.isEmpty()) {
            return new AvailabilitySnapshot(staff, Map.of());
        }

        List<Long> ids = staff.stream().map(User::getId).toList();
        List<BookingInterval> intervals = orderRepository.findBookedIntervalsForEmployees(ids, from, to);
        Map<Long, List<BookingInterval>> byEmployee = intervals.stream()
                .collect(Collectors.groupingBy(BookingInterval::employeeId));
        byEmployee.values().forEach(list -> list.sort(Comparator.comparing(BookingInterval::startDate)));
        return new AvailabilitySnapshot(staff, byEmployee);
    }

    private record AvailabilitySnapshot(List<User> staff, Map<Long, List<BookingInterval>> byEmployee) {
        List<User> available(Date start, Date end) {
            return staff.stream().filter(user -> isFree(user.getId(), start, end)).toList();
        }

        private boolean isFree(Long employeeId, Date start, Date end) {
            // Sorted by start time; intervals beginning after the candidate ends
            // cannot collide, so there is no need to inspect the rest.
            for (BookingInterval slot : byEmployee.getOrDefault(employeeId, List.of())) {
                if (!slot.startDate().before(end)) {
                    break;
                }
                if (slot.endDate().after(start)) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals(UserRole.ADMIN.authority()));
    }

    public DateRange convertToDateRange(LocalDate fromDate, LocalDate toDate) {
        return new DateRange(
                Date.from(LocalDateTime.of(fromDate, LocalTime.MIN).atZone(WARSAW_ZONE).toInstant()),
                Date.from(LocalDateTime.of(toDate, LocalTime.MAX).atZone(WARSAW_ZONE).toInstant())
        );
    }

    public List<Order> filterInProgressOrders(List<Order> orders) {
        return orders.stream()
                .filter(order -> pl.lukbol.dyplom.common.OrderStatus.IN_PROGRESS == order.getOrderStatus())
                .collect(Collectors.toList());
    }

    public User findUserByName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApplicationException.UserNotFoundException(Messages.USER_NOT_FOUND_BY_NAME + name);
        }
        List<User> users = userRepository.findByNameIgnoreCase(name.trim());
        if (users.isEmpty()) {
            throw new ApplicationException.UserNotFoundException(Messages.USER_NOT_FOUND_BY_NAME + name);
        }
        if (users.size() > 1) {
            throw new ApplicationException.AmbiguousEmployeeException(
                    Messages.AMBIGUOUS_EMPLOYEE + name + ". Użyj unikalnego identyfikatora.");
        }
        return users.get(0);
    }

    public void updateOrderFields(Order order, EditOrderDTO request,
                                  User selectedEmployee, DateRange dates) {
        // Reuse the employee and range validated by BookingSlotService.
        order.setEmployee(selectedEmployee);
        order.setDescription(request.description());
        order.setClientName(request.clientName());
        order.setClientEmail(request.email());
        order.setPhoneNumber(request.phoneNumber());

        order.setStartDate(dates.start());
        order.setEndDate(dates.end());

        order.setDuration(request.hours());
        order.setPrice(request.price());
        order.setStatus(request.status());
    }

    public void addNotificationAboutNewOrder(User user) {
        List<Notification> notifications = new ArrayList<>(user.getNotifications());
        notifications.add(new Notification(Messages.NEW_ORDER_NOTIF, new Date(), user, Messages.SYSTEM_NOTIFICATION_CREATOR));
        user.setNotifications(notifications);
        userRepository.save(user);
    }

    public List<Material> createMaterialsForOrder(List<String> items, Order order) {
        return items.stream()
                .map(item -> new Material(item, order, false))
                .collect(Collectors.toList());
    }

    public List<Order> findOrdersForAdmin(DateRange dateRange) {
        return orderRepository.findByEndDateBetween(dateRange.start(), dateRange.end());
    }

    public List<Order> findOrdersForUser(Long employeeId, DateRange dateRange) {
        return orderRepository.findByEmployeeIdAndEndDateBetween(employeeId, dateRange.start(), dateRange.end());
    }

    private final pl.lukbol.dyplom.services.OrderMapper orderMapper = new pl.lukbol.dyplom.services.OrderMapper();

    public OrderDTO toOrderDTO(Order order) { return orderMapper.toOrderDTO(order); }
    public OrderDetailsDTO buildOrderDetailsDTO(Order order) { return orderMapper.buildOrderDetailsDTO(order); }

    private final pl.lukbol.dyplom.services.SchedulingPlanner schedulingPlanner =
            new pl.lukbol.dyplom.services.SchedulingPlanner();

    public AvailabilityDTO findNextAvailableSlot(Date startDate, double durationHours,
            BiFunction<Calendar, Calendar, List<User>> availableUsersProvider) {
        return schedulingPlanner.findNextAvailableSlot(startDate, durationHours, availableUsersProvider);
    }

    public void updateOrderMaterials(Order order, List<String> items) {
        materialRepository.deleteAllByOrder(order);
        order.setMaterials(createMaterialsForOrder(items, order));
    }
}