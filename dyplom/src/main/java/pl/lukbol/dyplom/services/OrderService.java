package pl.lukbol.dyplom.services;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDate;
import java.util.*;

import static pl.lukbol.dyplom.utilities.OrderUtils.WARSAW_ZONE;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MaterialRepository materialRepository;
    private final OrderUtils orderUtils;
    private final BookingSlotService bookingSlotService;
    private final BookingPolicy bookingPolicy = new BookingPolicy();

    public CurrentDateDTO getCurrentDate() {
        return new CurrentDateDTO(LocalDate.now(WARSAW_ZONE).toString());
    }

    @Transactional
    public ApiResponseDTO addOrder(AddOrderDTO request) {
        User user = orderUtils.findUserByName(request.selectedUser());
        DateRange dates = DateUtils.parseOrderRange(request.startDate(), request.endDate());
        Date startDate = dates.start();
        Date endDate = dates.end();
        bookingPolicy.validateDuration(startDate, endDate, request.hours());

        // The availability preview is advisory; check again under a DB row lock.
        bookingSlotService.lockAndCheckSlot(user.getId(), startDate, endDate, null);

        Order newOrder = new Order(
                request.description(),
                request.clientName(),
                request.email(),
                request.phoneNumber(),
                user,
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
        log.info("Dodano zlecenie dla klienta {} - pracownik: {}", request.clientName(), request.selectedUser());

        return new ApiResponseDTO(Messages.NEW_ORDER_NOTIF);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getDailyOrders(Authentication authentication, OrderRequestByDateDTO orderRequestDTO) {
        String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals(UserRole.ADMIN.authority()));

        DateRange dateRange = parseDateRange(orderRequestDTO.startDate(), orderRequestDTO.endDate());
        User user = userRepository.findByEmail(userEmail);

        List<Order> orders = isAdmin
                ? orderUtils.findOrdersForAdmin(dateRange)
                : orderUtils.findOrdersForUser(user.getId(), dateRange);

        return orders.stream()
                .map(orderUtils::toOrderDTO)
                .toList();
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public OrderDetailsDTO getOrderDetails(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException(Messages.ORDER_NOT_FOUND));
        return orderUtils.buildOrderDetailsDTO(order);
    }

    @Transactional
    public ApiResponseDTO editOrder(Long id, EditOrderDTO request) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException(Messages.ORDER_NOT_FOUND));

        // Lock the destination employee, then validate the NEW interval,
        // excluding only this order's current booking.
        User selectedEmployee = orderUtils.findUserByName(request.selectedUser());
        DateRange range = DateUtils.parseOrderRange(request.startDate(), request.endDate());
        bookingPolicy.validateDuration(range.start(), range.end(), request.hours());
        bookingSlotService.lockAndCheckSlot(selectedEmployee.getId(), range.start(), range.end(), id);

        orderUtils.updateOrderFields(order, request, selectedEmployee, range);
        orderUtils.updateOrderMaterials(order, request.items());
        orderRepository.save(order);

        return new ApiResponseDTO(Messages.ORDER_UPDATED_NOTIF);
    }

    @Transactional(readOnly = true)
    public AvailabilityDTO checkAvailability(double durationHours, int startHour) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone(WARSAW_ZONE));
        now.set(Calendar.HOUR_OF_DAY, startHour);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);

        return orderUtils.findNextAvailableSlot(
                now.getTime(), durationHours,
                orderUtils.prepareAvailabilityLookup(now.getTime(), null, null)
        );
    }

    @Transactional(readOnly = true)
    public AvailabilityDTO checkAvailabilityNextDay(Long orderId, double durationHours) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException(Messages.ORDER_NOT_FOUND));

        return orderUtils.findNextAvailableSlot(
                order.getEndDate(), durationHours,
                orderUtils.prepareAvailabilityLookup(order.getEndDate(), order.getEmployee().getId(), null)
        );
    }

    @Transactional(readOnly = true)
    public AvailabilityDTO checkAvailabilityOtherEmployee(Long orderId, double durationHours) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException(Messages.ORDER_NOT_FOUND));

        return orderUtils.findNextAvailableSlot(
                order.getEndDate(), durationHours,
                orderUtils.prepareAvailabilityLookup(order.getEndDate(), null, order.getEmployee().getId())
        );
    }

    @Transactional
    public ApiResponseDTO deleteOrder(Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException(Messages.ORDER_NOT_FOUND));

        if (order.getMaterials() != null) {
            materialRepository.deleteAll(order.getMaterials());
        }

        orderRepository.delete(order);
        log.info("Usunieto zlecenie id={}", id);

        return new ApiResponseDTO(Messages.ORDER_DELETED);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> searchOrdersByStartDateBetweenWithMaterials(LocalDate fromDate, LocalDate toDate, Authentication authentication) {
        boolean isAdmin = orderUtils.isAdmin(authentication);
        DateRange dateRange = orderUtils.convertToDateRange(fromDate, toDate);
        String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());

        List<Order> matchingOrders = isAdmin
                ? orderRepository.findByStartDateBetweenWithMaterials(dateRange.start(), dateRange.end())
                : orderRepository.findByEmployeeIdAndStartDateBetweenWithMaterials(
                userRepository.findByEmail(userEmail).getId(),
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
        return DateUtils.parseSearchRange(start, end);
    }
}
