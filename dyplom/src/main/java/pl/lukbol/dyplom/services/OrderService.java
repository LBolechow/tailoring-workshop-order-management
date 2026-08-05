package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.common.UserRole;
import pl.lukbol.dyplom.DTOs.date.CurrentDateDTO;
import pl.lukbol.dyplom.DTOs.date.DateRange;
import pl.lukbol.dyplom.DTOs.order.*;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.classes.Material;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;
import pl.lukbol.dyplom.utilities.DateUtils;
import pl.lukbol.dyplom.utilities.OrderUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static pl.lukbol.dyplom.utilities.OrderUtils.WARSAW_ZONE;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MaterialRepository materialRepository;
    private final OrderUtils orderUtils;

    public CurrentDateDTO getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return new CurrentDateDTO(sdf.format(new Date()));
    }

    @Transactional
    public ApiResponseDTO addOrder(AddOrderDTO request) {
        User user = orderUtils.findUserByName(request.selectedUser());
        Date startDate;
        Date endDate;
        try {
            startDate = DateUtils.parseDate(request.startDate(), "yyyy-MM-dd");
            endDate = DateUtils.parseDate(request.endDate(), "yyyy-MM-dd");
        } catch (ParseException e) {
            throw new ApplicationException.InvalidDateException("Nieprawidłowy format daty: " + e.getMessage());
        }

        Order newOrder = new Order(
                request.description(),
                request.clientName(),
                request.email(),
                request.phoneNumber(),
                request.selectedUser(),
                startDate,
                endDate,
                request.status(),
                request.price(),
                request.hours(),
                null,
                UUID.randomUUID().toString()
        );

        orderUtils.addNotificationAboutNewOrder(user);

        List<Material> materials = orderUtils.createMaterialsForOrder(request.items(), newOrder);
        newOrder.setMaterials(materials);
        orderRepository.save(newOrder);

        return new ApiResponseDTO(Messages.NEW_ORDER_NOTIF);
    }

    public List<OrderDTO> getDailyOrders(Authentication authentication, OrderRequestByDateDTO orderRequestDTO) {
        String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals(UserRole.ADMIN.authority()));

        DateRange dateRange = parseDateRange(orderRequestDTO.startDate(), orderRequestDTO.endDate());
        User user = userRepository.findByEmail(userEmail);

        List<Order> orders = isAdmin
                ? orderUtils.findOrdersForAdmin(dateRange)
                : orderUtils.findOrdersForUser(user.getName(), dateRange);

        return orders.stream()
                .map(orderUtils::toOrderDTO)
                .toList();
    }

    public List<OrderDTO> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new UsernameNotFoundException(Messages.USER_NOT_FOUND_BY_EMAIL);
        }

        List<Order> userOrders = orderRepository.findOrdersByUserEmail(user.getEmail());
        userOrders.sort(Comparator.comparing(Order::getEndDate).reversed());
        return userOrders.stream()
                .map(orderUtils::toOrderDTO)
                .toList();
    }

    public OrderDetailsDTO getOrderDetails(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException(Messages.ORDER_NOT_FOUND));
        return orderUtils.buildOrderDetailsDTO(order);
    }

    @Transactional
    public ApiResponseDTO editOrder(Long id, EditOrderDTO request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException(Messages.ORDER_NOT_FOUND));

        orderUtils.updateOrderFields(order, request);
        orderUtils.updateOrderMaterials(order, request.items());
        orderRepository.save(order);

        return new ApiResponseDTO(Messages.ORDER_UPDATED_NOTIF);
    }

    public AvailabilityDTO checkAvailability(double durationHours, int startHour) {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, startHour);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);

        return orderUtils.findNextAvailableSlot(
                now.getTime(),
                durationHours,
                (current, end) -> orderUtils.findAvailableUsersWithEndDateTime(
                        current.getTime(),
                        end.getTime(),
                        (int) (durationHours * 60)
                )
        );
    }

    public AvailabilityDTO checkAvailabilityNextDay(Long orderId, double durationHours) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException("Zamówienie nie znalezione"));

        return orderUtils.findNextAvailableSlot(
                order.getEndDate(),
                durationHours,
                (current, end) -> {
                    User user = userRepository.findByName(order.getEmployeeName());
                    return orderUtils.findAvailableUserWithEndDateTime(
                            user.getId(),
                            current.getTime(),
                            end.getTime(),
                            (int) (durationHours * 60)
                    );
                }
        );
    }

    public AvailabilityDTO checkAvailabilityOtherEmployee(Long orderId, double durationHours) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException("Zamówienie nie znalezione"));

        return orderUtils.findNextAvailableSlot(
                order.getEndDate(),
                durationHours,
                (current, end) -> orderUtils.findAvailableUsersWithoutEmployee(
                        order.getId(),
                        current.getTime(),
                        end.getTime(),
                        (int) (durationHours * 60)
                )
        );
    }

    @Transactional
    public ApiResponseDTO deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException(Messages.ORDER_NOT_FOUND));

        if (order.getMaterials() != null) {
            materialRepository.deleteAll(order.getMaterials());
        }

        orderRepository.delete(order);
        return new ApiResponseDTO(Messages.ORDER_DELETED);
    }

    public List<OrderDTO> searchOrdersByStartDateBetweenWithMaterials(LocalDate fromDate, LocalDate toDate, Authentication authentication) {
        boolean isAdmin = orderUtils.isAdmin(authentication);
        DateRange dateRange = orderUtils.convertToDateRange(fromDate, toDate);
        String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());

        List<Order> matchingOrders = isAdmin
                ? orderRepository.findByStartDateBetweenWithMaterials(dateRange.start(), dateRange.end())
                : orderRepository.findByEmployeeNameAndStartDateBetweenWithMaterials(
                userRepository.findByEmail(userEmail).getName(),
                dateRange.start(),
                dateRange.end()
        );

        return orderUtils.filterInProgressOrders(matchingOrders).stream()
                .map(orderUtils::toOrderDTO)
                .toList();
    }

    @Transactional
    public ApiResponseDTO updateMaterialCheckedState(Long materialId, boolean checked) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ApplicationException.MaterialNotFoundException(
                        Messages.MATERIAL_NOT_FOUND + " (id: " + materialId + ")"
                ));

        material.setChecked(checked);
        materialRepository.save(material);

        return new ApiResponseDTO(Messages.MATERIAL_UPDATED);
    }

    private DateRange parseDateRange(String start, String end) {
        if (start == null || end == null) {
            LocalDateTime now = LocalDateTime.now();
            return new DateRange(
                    Date.from(now.with(LocalTime.MIN).atZone(WARSAW_ZONE).toInstant()),
                    Date.from(now.with(LocalTime.MAX).atZone(WARSAW_ZONE).toInstant())
            );
        }

        try {
            LocalDateTime startDateTime = LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime endDateTime = LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME);
            return new DateRange(
                    Date.from(startDateTime.atZone(WARSAW_ZONE).toInstant()),
                    Date.from(endDateTime.atZone(WARSAW_ZONE).toInstant())
            );
        } catch (DateTimeParseException e) {
            return new DateRange(
                    Date.from(LocalDate.parse(start).atStartOfDay(WARSAW_ZONE).toInstant()),
                    Date.from(LocalDate.parse(end).atTime(23, 59, 59).atZone(WARSAW_ZONE).toInstant())
            );
        }
    }
}