package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.lukbol.dyplom.DTOs.date.CurrentDateDTO;
import pl.lukbol.dyplom.DTOs.order.*;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.classes.Material;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.OrderService;
import pl.lukbol.dyplom.utilities.OrderUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private OrderUtils orderUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Order order;
    private Material material;

    @BeforeEach
    void setUp() {
        user = new User("Jan Kowalski", "jan@test.pl", "haslo", true);
        user.setId(1L);

        material = new Material("Materiał testowy", null, false);
        material.setId(1L);

        order = new Order(
                "Opis zlecenia",
                "Klient Test",
                "klient@test.pl",
                "123456789",
                "Jan Kowalski",
                new Date(),
                new Date(),
                "W trakcie",
                100,
                2.0,
                new ArrayList<>(List.of(material)),
                "TEST-CODE-123"
        );
        order.setId(1L);
    }

    // getCurrentDate

    @Test
    void getCurrentDate_shouldReturnCurrentDateInCorrectFormat() {
        CurrentDateDTO result = orderService.getCurrentDate();

        assertThat(result.currentDate()).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    // addOrder

    @Test
    void addOrder_shouldSaveOrderAndReturnSuccessMessage() {
        AddOrderDTO dto = new AddOrderDTO(
                "Opis", "Klient", "klient@test.pl", "123456789",
                2.0, "2024-01-15", "2024-01-16", "Jan Kowalski", 100, "W trakcie",
                List.of("Materiał 1", "Materiał 2")
        );
        when(orderUtils.findUserByName("Jan Kowalski")).thenReturn(user);
        when(orderUtils.createMaterialsForOrder(any(), any())).thenReturn(List.of(material));

        ApiResponseDTO result = orderService.addOrder(dto);

        verify(orderUtils).addNotificationAboutNewOrder(user);
        verify(orderRepository).save(any(Order.class));
        assertThat(result.message()).isEqualTo(Messages.NEW_ORDER_NOTIF);
    }

    @Test
    void addOrder_shouldThrowInvalidDateException_whenDateFormatIsWrong() {
        AddOrderDTO dto = new AddOrderDTO(
                "Opis", "Klient", "klient@test.pl", "123456789",
                2.0, "zly-format", "2024-01-16", "Jan Kowalski", 100, "W trakcie",
                List.of()
        );
        when(orderUtils.findUserByName("Jan Kowalski")).thenReturn(user);

        assertThatThrownBy(() -> orderService.addOrder(dto))
                .isInstanceOf(ApplicationException.InvalidDateException.class);

        verify(orderRepository, never()).save(any());
    }

    // getUserOrders

    @Test
    void getUserOrders_shouldReturnSortedOrders() {
        Order olderOrder = new Order(
                "Starsze", "Klient", "klient@test.pl", "123",
                "Jan Kowalski", new Date(1000), new Date(1000),
                "W trakcie", 50, 1.0, new ArrayList<>(), "CODE-1"
        );
        Order newerOrder = new Order(
                "Nowsze", "Klient", "klient@test.pl", "123",
                "Jan Kowalski", new Date(9000), new Date(9000),
                "W trakcie", 50, 1.0, new ArrayList<>(), "CODE-2"
        );

        when(userRepository.findByEmail("jan@test.pl")).thenReturn(user);
        when(orderRepository.findOrdersByUserEmail("jan@test.pl")).thenReturn(new ArrayList<>(List.of(olderOrder, newerOrder)));
        when(orderUtils.toOrderDTO(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return new OrderDTO(null, o.getDescription(), null, null, null, 0, null, null, null, 0, null, null);
        });

        List<OrderDTO> result = orderService.getUserOrders("jan@test.pl");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).description()).isEqualTo("Nowsze");
        assertThat(result.get(1).description()).isEqualTo("Starsze");
    }

    @Test
    void getUserOrders_shouldThrowUsernameNotFoundException_whenUserNotFound() {
        when(userRepository.findByEmail("nieistnieje@test.pl")).thenReturn(null);

        assertThatThrownBy(() -> orderService.getUserOrders("nieistnieje@test.pl"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(Messages.USER_NOT_FOUND_BY_EMAIL);
    }

    // getOrderDetails

    @Test
    void getOrderDetails_shouldReturnOrderDetails() {
        OrderDetailsDTO expectedDTO = new OrderDetailsDTO(
                1L, "Opis", "Klient", "klient@test.pl", "123",
                "Jan Kowalski", "W trakcie", 100, 2.0, "TEST-CODE", List.of()
        );
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderUtils.buildOrderDetailsDTO(order)).thenReturn(expectedDTO);

        OrderDetailsDTO result = orderService.getOrderDetails(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.description()).isEqualTo("Opis");
    }

    @Test
    void getOrderDetails_shouldThrowOrderNotFoundException_whenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderDetails(99L))
                .isInstanceOf(ApplicationException.OrderNotFoundException.class)
                .hasMessage(Messages.ORDER_NOT_FOUND);
    }

    // editOrder

    @Test
    void editOrder_shouldUpdateOrderAndReturnSuccessMessage() {
        EditOrderDTO dto = new EditOrderDTO(
                "Nowy opis", "Nowy klient", "nowy@test.pl", "987654321",
                3.0, "2024-02-01", "2024-02-02", "Jan Kowalski", 200, "Zakończone",
                List.of("Nowy materiał")
        );
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ApiResponseDTO result = orderService.editOrder(1L, dto);

        verify(orderUtils).updateOrderFields(order, dto);
        verify(orderUtils).updateOrderMaterials(order, dto.items());
        verify(orderRepository).save(order);
        assertThat(result.message()).isEqualTo(Messages.ORDER_UPDATED_NOTIF);
    }

    @Test
    void editOrder_shouldThrowOrderNotFoundException_whenOrderNotFound() {
        EditOrderDTO dto = new EditOrderDTO(
                "Opis", "Klient", "klient@test.pl", "123",
                2.0, "2024-01-15", "2024-01-16", "Jan Kowalski", 100, "W trakcie",
                List.of()
        );
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.editOrder(99L, dto))
                .isInstanceOf(ApplicationException.OrderNotFoundException.class)
                .hasMessage(Messages.ORDER_NOT_FOUND);

        verify(orderRepository, never()).save(any());
    }

    // deleteOrder

    @Test
    void deleteOrder_shouldDeleteOrderWithMaterialsAndReturnSuccessMessage() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ApiResponseDTO result = orderService.deleteOrder(1L);

        verify(materialRepository).deleteAll(order.getMaterials());
        verify(orderRepository).delete(order);
        assertThat(result.message()).isEqualTo(Messages.ORDER_DELETED);
    }

    @Test
    void deleteOrder_shouldDeleteOrderWithoutCallingMaterialDelete_whenNoMaterials() {
        order.setMaterials(null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ApiResponseDTO result = orderService.deleteOrder(1L);

        verify(materialRepository, never()).deleteAll(any());
        verify(orderRepository).delete(order);
        assertThat(result.message()).isEqualTo(Messages.ORDER_DELETED);
    }

    @Test
    void deleteOrder_shouldThrowOrderNotFoundException_whenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(99L))
                .isInstanceOf(ApplicationException.OrderNotFoundException.class)
                .hasMessage(Messages.ORDER_NOT_FOUND);

        verify(orderRepository, never()).delete(any());
    }

    // updateMaterialCheckedState

    @Test
    void updateMaterialCheckedState_shouldUpdateCheckedAndReturnSuccessMessage() {
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        ApiResponseDTO result = orderService.updateMaterialCheckedState(1L, true);

        assertThat(material.isChecked()).isTrue();
        verify(materialRepository).save(material);
        assertThat(result.message()).isEqualTo(Messages.MATERIAL_UPDATED);
    }

    @Test
    void updateMaterialCheckedState_shouldThrowMaterialNotFoundException_whenMaterialNotFound() {
        when(materialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateMaterialCheckedState(99L, true))
                .isInstanceOf(ApplicationException.MaterialNotFoundException.class)
                .hasMessageContaining(Messages.MATERIAL_NOT_FOUND);

        verify(materialRepository, never()).save(any());
    }

    // getDailyOrders — admin vs pracownik

    @Test
    void getDailyOrders_shouldFetchAllOrders_whenAdmin() {
        Collection authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("admin@test.pl");
        when(authentication.getAuthorities()).thenReturn(authorities);
        when(userRepository.findByEmail("admin@test.pl")).thenReturn(user);
        when(orderUtils.findOrdersForAdmin(any())).thenReturn(List.of(order));
        when(orderUtils.toOrderDTO(any())).thenReturn(
                new OrderDTO(1L, "Opis", null, null, null, 0, null, null, null, 0, null, null)
        );

        OrderRequestByDateDTO requestDTO = new OrderRequestByDateDTO(null, null);
        List<OrderDTO> result = orderService.getDailyOrders(authentication, requestDTO);

        verify(orderUtils).findOrdersForAdmin(any());
        verify(orderUtils, never()).findOrdersForUser(any(), any());
        assertThat(result).hasSize(1);
    }

    @Test
    void getDailyOrders_shouldFetchUserOrders_whenNotAdmin() {
        Collection authorities = List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("jan@test.pl");
        when(authentication.getAuthorities()).thenReturn(authorities);
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(user);
        when(orderUtils.findOrdersForUser(any(), any())).thenReturn(List.of(order));
        when(orderUtils.toOrderDTO(any())).thenReturn(
                new OrderDTO(1L, "Opis", null, null, null, 0, null, null, null, 0, null, null)
        );

        OrderRequestByDateDTO requestDTO = new OrderRequestByDateDTO(null, null);
        List<OrderDTO> result = orderService.getDailyOrders(authentication, requestDTO);

        verify(orderUtils).findOrdersForUser(eq("Jan Kowalski"), any());
        verify(orderUtils, never()).findOrdersForAdmin(any());
        assertThat(result).hasSize(1);
    }

    // searchOrdersByStartDateBetweenWithMaterials

    @Test
    void searchOrders_shouldReturnFilteredOrders_whenAdmin() {
        Collection authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("admin@test.pl");
        when(orderUtils.isAdmin(authentication)).thenReturn(true);
        when(orderUtils.convertToDateRange(any(), any())).thenReturn(
                new pl.lukbol.dyplom.DTOs.date.DateRange(new Date(), new Date())
        );
        when(orderRepository.findByStartDateBetweenWithMaterials(any(), any())).thenReturn(List.of(order));
        when(orderUtils.filterInProgressOrders(any())).thenReturn(List.of(order));
        when(orderUtils.toOrderDTO(any())).thenReturn(
                new OrderDTO(1L, "Opis", null, null, null, 0, null, null, null, 0, null, null)
        );

        List<OrderDTO> result = orderService.searchOrdersByStartDateBetweenWithMaterials(
                LocalDate.now(), LocalDate.now(), authentication
        );

        assertThat(result).hasSize(1);
        verify(orderRepository).findByStartDateBetweenWithMaterials(any(), any());
    }
}