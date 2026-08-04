package pl.lukbol.dyplom.utilities;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.DTOs.order.OrderDetailsDTO;
import pl.lukbol.dyplom.common.Messages;
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

    public enum UserRoles {
        ROLE_ADMIN, ROLE_EMPLOYEE, ROLE_CLIENT
    }

    public static final ZoneId WARSAW_ZONE = ZoneId.of("Europe/Warsaw");

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MaterialRepository materialRepository;

    public boolean isWorkingDay(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY;
    }

    public List<User> findAvailableUsersWithEndDateTime(Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        if (taskStartDateTime == null || taskEndDateTime == null) {
            throw new IllegalArgumentException(Messages.NULL_DATE_EXCEPTION);
        }

        List<String> roleNamesToSearch = Arrays.asList(UserRoles.ROLE_EMPLOYEE.name(), UserRoles.ROLE_ADMIN.name());
        List<User> availableUsers = new ArrayList<>();

        for (User user : userRepository.findAll()) {
            if (user.getRole() == null || !roleNamesToSearch.contains(user.getRole().getName())) {
                continue;
            }

            List<Order> userOrders = orderRepository
                    .findByEmployeeNameAndEndDateAfterAndStartDateBefore(
                            user.getName(), taskStartDateTime, taskEndDateTime
                    );

            boolean isAvailable = userOrders.stream().noneMatch(order ->
                    hasTimeOverlap(taskStartDateTime, taskEndDateTime, order.getStartDate(), order.getEndDate())
            );

            if (isAvailable) {
                availableUsers.add(user);
            }
        }

        return availableUsers;
    }

    public List<User> findAvailableUserWithEndDateTime(Long employeeId, Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        if (taskStartDateTime == null || taskEndDateTime == null) {
            throw new IllegalArgumentException(Messages.NULL_DATE_EXCEPTION);
        }

        List<String> roleNamesToSearch = Arrays.asList(UserRoles.ROLE_EMPLOYEE.name(), UserRoles.ROLE_ADMIN.name());
        List<User> availableUsers = new ArrayList<>();

        userRepository.findById(employeeId).ifPresent(user -> {
            if (user.getRole() != null && roleNamesToSearch.contains(user.getRole().getName())) {
                List<Order> userOrders = orderRepository
                        .findByEmployeeNameAndEndDateAfterAndStartDateBefore(
                                user.getName(), taskStartDateTime, taskEndDateTime
                        );

                boolean isAvailable = userOrders.stream().noneMatch(order ->
                        hasTimeOverlap(taskStartDateTime, taskEndDateTime, order.getStartDate(), order.getEndDate())
                );

                if (isAvailable) {
                    availableUsers.add(user);
                }
            }
        });

        return availableUsers;
    }

    public List<User> findAvailableUsersWithoutEmployee(Long orderId, Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        if (taskStartDateTime == null || taskEndDateTime == null) {
            throw new IllegalArgumentException(Messages.NULL_DATE_EXCEPTION);
        }

        List<String> roleNamesToSearch = Arrays.asList(UserRoles.ROLE_EMPLOYEE.name(), UserRoles.ROLE_ADMIN.name());
        List<User> availableUsers = new ArrayList<>();

        orderRepository.findById(orderId).ifPresent(order -> {
            String employeeNameOnOrder = order.getEmployeeName();
            List<User> allUsersExceptEmployee = userRepository.findAllByNameNot(employeeNameOnOrder);

            for (User user : allUsersExceptEmployee) {
                if (user.getRole() == null || !roleNamesToSearch.contains(user.getRole().getName())) {
                    continue;
                }

                List<Order> userOrders = orderRepository.findByEmployeeNameAndEndDateAfterAndStartDateBefore(
                        user.getName(), taskStartDateTime, taskEndDateTime
                );

                boolean isAvailable = userOrders.stream().noneMatch(userOrder ->
                        hasTimeOverlap(taskStartDateTime, taskEndDateTime, userOrder.getStartDate(), userOrder.getEndDate())
                );

                if (isAvailable) {
                    availableUsers.add(user);
                }
            }
        });

        return availableUsers;
    }

    private boolean hasTimeOverlap(Date start1, Date end1, Date start2, Date end2) {
        return (end1.after(start2) && end1.before(end2)) ||
                (start1.after(start2) && start1.before(end2)) ||
                (start1.before(start2) && end1.after(end2));
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals(UserRoles.ROLE_ADMIN.name()));
    }

    public DateRange convertToDateRange(LocalDate fromDate, LocalDate toDate) {
        return new DateRange(
                Date.from(LocalDateTime.of(fromDate, LocalTime.MIN).atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(LocalDateTime.of(toDate, LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    public List<Order> filterInProgressOrders(List<Order> orders) {
        return orders.stream()
                .filter(order -> Messages.ORDER_STATUS_IN_PROGRESS.equals(order.getStatus()))
                .collect(Collectors.toList());
    }

    public User findUserByName(String name) {
        List<User> users = userRepository.findByNameContainingIgnoreCase(name);
        if (users.isEmpty()) {
            throw new ApplicationException.UserNotFoundException(Messages.USER_NOT_FOUND_BY_NAME + name);
        }
        return users.get(0);
    }

    public void updateOrderFields(Order order, EditOrderDTO request) {
        User user = findUserByName(request.selectedUser());
        order.setEmployeeName(user.getName());
        order.setDescription(request.description());
        order.setClientName(request.clientName());
        order.setClientEmail(request.email());
        order.setPhoneNumber(request.phoneNumber());

        try {
            order.setStartDate(DateUtils.parseDate(request.startDate(), "yyyy-MM-dd"));
            order.setEndDate(DateUtils.parseDate(request.endDate(), "yyyy-MM-dd"));
        } catch (ParseException e) {
            throw new ApplicationException.InvalidDateException(Messages.INVALID_DATE_FORMAT + e.getMessage());
        }

        order.setDuration(request.hours());
        order.setPrice(request.price());
        order.setStatus(request.status());
    }

    public void addNotificationAboutNewOrder(User user) {
        List<Notification> notifications = new ArrayList<>(user.getNotifications());
        notifications.add(new Notification(Messages.NEW_ORDER_NOTIF, new Date(), user, "System"));
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

    public List<Order> findOrdersForUser(String username, DateRange dateRange) {
        return orderRepository.findByEmployeeNameAndEndDateBetween(username, dateRange.start(), dateRange.end());
    }

    public OrderDTO toOrderDTO(Order order) {
        return new OrderDTO(
                order.getId(),
                order.getDescription(),
                order.getClientName(),
                order.getClientEmail(),
                order.getPhoneNumber(),
                order.getDuration(),
                order.getStartDate().toString(),
                order.getEndDate().toString(),
                order.getEmployeeName(),
                order.getPrice(),
                order.getStatus(),
                order.getMaterials().stream()
                        .map(m -> new MaterialDTO(m.getId(), m.getItem(), m.isChecked()))
                        .toList()
        );
    }

    public OrderDetailsDTO buildOrderDetailsDTO(Order order) {
        List<MaterialDTO> materials = order.getMaterials().stream()
                .map(m -> new MaterialDTO(m.getId(), m.getItem(), m.isChecked()))
                .toList();

        return new OrderDetailsDTO(
                order.getId(),
                order.getDescription(),
                order.getClientName(),
                order.getClientEmail(),
                order.getPhoneNumber(),
                order.getEmployeeName(),
                order.getStatus(),
                order.getPrice(),
                order.getDuration(),
                order.getIdCode(),
                materials
        );
    }


    public AvailabilityDTO findNextAvailableSlot(
            Date startDate,
            double durationHours,
            BiFunction<Calendar, Calendar, List<User>> availableUsersProvider) {

        Calendar currentDateTime = Calendar.getInstance();
        currentDateTime.setTime(startDate);

        final int WORKDAY_END_HOUR = 16;
        final int START_HOUR = 8;
        final int MAX_DAYS_LOOKAHEAD = 30;
        int daysChecked = 0;

        if (currentDateTime.get(Calendar.HOUR_OF_DAY) >= WORKDAY_END_HOUR) {
            advanceToNextDayStart(currentDateTime, START_HOUR);
        }

        while (daysChecked < MAX_DAYS_LOOKAHEAD) {
            if (isWorkingDay(currentDateTime)) {
                Calendar endDateTime = (Calendar) currentDateTime.clone();
                int durationMinutes = (int) (durationHours * 60);
                endDateTime.add(Calendar.MINUTE, durationMinutes);

                if (endDateTime.get(Calendar.HOUR_OF_DAY) > WORKDAY_END_HOUR) {
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
            }

            advanceToNextDayStart(currentDateTime, START_HOUR);
            daysChecked++;
        }

        return new AvailabilityDTO(
                false,
                Messages.NO_AVAILABLE_EMPLOYEES,
                null,
                null,
                null
        );
    }

    private void advanceToNextDayStart(Calendar calendar, int startHour) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
    }

    public void updateOrderMaterials(Order order, List<String> items) {
        materialRepository.deleteAllByOrder(order);
        order.setMaterials(createMaterialsForOrder(items, order));
    }
}