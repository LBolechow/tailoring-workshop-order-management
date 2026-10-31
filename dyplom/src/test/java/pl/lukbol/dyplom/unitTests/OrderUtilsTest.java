package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pl.lukbol.dyplom.DTOs.date.DateRange;
import pl.lukbol.dyplom.DTOs.order.*;
import pl.lukbol.dyplom.DTOs.order.BookingInterval;
import pl.lukbol.dyplom.classes.Material;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.OrderUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderUtilsTest {

    private static final String DATE_ONLY = "yyyy-MM-dd";

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderUtils orderUtils;

    private User employeeUser;
    private User adminUser;
    private Role roleEmployee;
    private Role roleAdmin;
    private Order order;
    private Material material;

    @BeforeEach
    void setUp() {
        roleEmployee = new Role("ROLE_EMPLOYEE");
        roleAdmin = new Role("ROLE_ADMIN");

        employeeUser = new User("Anna Nowak", "anna@test.pl", "haslo", true);
        employeeUser.setId(1L);
        employeeUser.setRole(roleEmployee);
        employeeUser.setNotifications(new ArrayList<>());

        adminUser = new User("Admin", "admin@test.pl", "haslo", true);
        adminUser.setId(2L);
        adminUser.setRole(roleAdmin);

        material = new Material("Materiał testowy", null, false);
        material.setId(1L);

        order = new Order(
                "Opis zlecenia", "Klient", "klient@test.pl", "123456789",
                employeeUser, new Date(), new Date(),
                "W trakcie", 100, 2.0, new ArrayList<>(List.of(material)), "CODE-123"
        );
        order.setId(1L);
    }

    /**
     * Zwraca najbliższy przyszły dzień o podanym dniu tygodnia i godzinie.
     * Daty muszą być w przyszłości, bo findNextAvailableSlot nie proponuje
     * terminów wstecz. Wyliczanie względem "teraz" zamiast hardkodowania
     * sprawia też, że test nie zestarzeje się z upływem czasu.
     */
    private Calendar futureDayAt(int dayOfWeek, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        while (cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private String asDate(Calendar cal) {
        return new SimpleDateFormat(DATE_ONLY).format(cal.getTime());
    }

    // isWorkingDay

    @Test
    void isWorkingDay_shouldReturnTrue_forMonday() {
        Calendar monday = Calendar.getInstance();
        monday.set(2024, Calendar.JANUARY, 15); // poniedziałek
        assertThat(orderUtils.isWorkingDay(monday)).isTrue();
    }

    @Test
    void isWorkingDay_shouldReturnTrue_forFriday() {
        Calendar friday = Calendar.getInstance();
        friday.set(2024, Calendar.JANUARY, 19); // piątek
        assertThat(orderUtils.isWorkingDay(friday)).isTrue();
    }

    @Test
    void isWorkingDay_shouldReturnFalse_forSaturday() {
        Calendar saturday = Calendar.getInstance();
        saturday.set(2024, Calendar.JANUARY, 20); // sobota
        assertThat(orderUtils.isWorkingDay(saturday)).isFalse();
    }

    @Test
    void isWorkingDay_shouldReturnFalse_forSunday() {
        Calendar sunday = Calendar.getInstance();
        sunday.set(2024, Calendar.JANUARY, 21); // niedziela
        assertThat(orderUtils.isWorkingDay(sunday)).isFalse();
    }

    // filterInProgressOrders

    @Test
    void filterInProgressOrders_shouldReturnOnlyInProgressOrders() {
        Order inProgress = new Order();
        inProgress.setStatus("W trakcie");

        Order finished = new Order();
        finished.setStatus("Zakończone");

        List<Order> result = orderUtils.filterInProgressOrders(List.of(inProgress, finished));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("W trakcie");
    }

    @Test
    void filterInProgressOrders_shouldReturnEmptyList_whenNoInProgressOrders() {
        Order finished = new Order();
        finished.setStatus("Zakończone");

        List<Order> result = orderUtils.filterInProgressOrders(List.of(finished));

        assertThat(result).isEmpty();
    }

    // findUserByName

    @Test
    void findUserByName_shouldReturnExactCaseInsensitiveMatch() {
        when(userRepository.findByNameIgnoreCase("Anna Nowak")).thenReturn(List.of(employeeUser));

        User result = orderUtils.findUserByName("Anna Nowak");

        assertThat(result.getName()).isEqualTo("Anna Nowak");
    }

    @Test
    void findUserByName_shouldThrowUserNotFoundException_whenNoUserFound() {
        when(userRepository.findByNameIgnoreCase("Nieznany")).thenReturn(List.of());

        assertThatThrownBy(() -> orderUtils.findUserByName("Nieznany"))
                .isInstanceOf(ApplicationException.UserNotFoundException.class)
                .hasMessageContaining(Messages.USER_NOT_FOUND_BY_NAME);
    }

    @Test
    void findUserByName_shouldRejectAmbiguousDisplayName() {
        User secondEmployee = new User("Anna Nowak", "anna2@test.pl", "hash", true);
        when(userRepository.findByNameIgnoreCase("Anna Nowak"))
                .thenReturn(List.of(employeeUser, secondEmployee));

        assertThatThrownBy(() -> orderUtils.findUserByName("Anna Nowak"))
                .isInstanceOf(ApplicationException.AmbiguousEmployeeException.class);
    }

    // createMaterialsForOrder

    @Test
    void createMaterialsForOrder_shouldCreateMaterialForEachItem() {
        List<String> items = List.of("Materiał A", "Materiał B", "Materiał C");

        List<Material> result = orderUtils.createMaterialsForOrder(items, order);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getItem()).isEqualTo("Materiał A");
        assertThat(result.get(1).getItem()).isEqualTo("Materiał B");
        assertThat(result.get(2).getItem()).isEqualTo("Materiał C");
        result.forEach(m -> {
            assertThat(m.isChecked()).isFalse();
            assertThat(m.getOrder()).isEqualTo(order);
        });
    }

    @Test
    void createMaterialsForOrder_shouldReturnEmptyList_whenNoItems() {
        List<Material> result = orderUtils.createMaterialsForOrder(List.of(), order);
        assertThat(result).isEmpty();
    }

    // updateOrderMaterials

    @Test
    void updateOrderMaterials_shouldDeleteOldAndSetNewMaterials() {
        List<String> newItems = List.of("Nowy materiał");

        orderUtils.updateOrderMaterials(order, newItems);

        verify(materialRepository).deleteAllByOrder(order);
        assertThat(order.getMaterials()).hasSize(1);
        assertThat(order.getMaterials().get(0).getItem()).isEqualTo("Nowy materiał");
    }

    // addNotificationAboutNewOrder

    @Test
    void addNotificationAboutNewOrder_shouldAddNotificationAndSaveUser() {
        orderUtils.addNotificationAboutNewOrder(employeeUser);

        assertThat(employeeUser.getNotifications()).hasSize(1);
        assertThat(employeeUser.getNotifications().get(0).getDescription()).isEqualTo(Messages.NEW_ORDER_NOTIF);
        assertThat(employeeUser.getNotifications().get(0).getCreator()).isEqualTo("System");
        verify(userRepository).save(employeeUser);
    }

    // toOrderDTO

    @Test
    void toOrderDTO_shouldMapAllFieldsCorrectly() {
        OrderDTO result = orderUtils.toOrderDTO(order);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.description()).isEqualTo("Opis zlecenia");
        assertThat(result.clientName()).isEqualTo("Klient");
        assertThat(result.email()).isEqualTo("klient@test.pl");
        assertThat(result.phoneNumber()).isEqualTo("123456789");
        assertThat(result.selectedUser()).isEqualTo("Anna Nowak");
        assertThat(result.price()).isEqualTo(100);
        assertThat(result.status()).isEqualTo("W trakcie");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).item()).isEqualTo("Materiał testowy");
    }

    // buildOrderDetailsDTO

    @Test
    void buildOrderDetailsDTO_shouldMapAllFieldsCorrectly() {
        OrderDetailsDTO result = orderUtils.buildOrderDetailsDTO(order);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.description()).isEqualTo("Opis zlecenia");
        assertThat(result.clientName()).isEqualTo("Klient");
        assertThat(result.clientEmail()).isEqualTo("klient@test.pl");
        assertThat(result.employeeName()).isEqualTo("Anna Nowak");
        assertThat(result.status()).isEqualTo("W trakcie");
        assertThat(result.price()).isEqualTo(100);
        assertThat(result.idCode()).isEqualTo("CODE-123");
        assertThat(result.materials()).hasSize(1);
    }

    // isAdmin

    @Test
    void isAdmin_shouldReturnTrue_whenUserHasAdminRole() {
        when(authentication.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        assertThat(orderUtils.isAdmin(authentication)).isTrue();
    }

    @Test
    void isAdmin_shouldReturnFalse_whenUserDoesNotHaveAdminRole() {
        when(authentication.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        assertThat(orderUtils.isAdmin(authentication)).isFalse();
    }

    // convertToDateRange

    @Test
    void convertToDateRange_shouldReturnDateRangeWithStartOfDayAndEndOfDay() {
        LocalDate from = LocalDate.of(2024, 1, 15);
        LocalDate to = LocalDate.of(2024, 1, 20);

        DateRange result = orderUtils.convertToDateRange(from, to);

        assertThat(result.start()).isNotNull();
        assertThat(result.end()).isNotNull();
        assertThat(result.start()).isBefore(result.end());
    }

    // findAvailableUsersWithEndDateTime

    @Test
    void findAvailableUsersWithEndDateTime_shouldThrowIllegalArgumentException_whenStartDateIsNull() {
        assertThatThrownBy(() -> orderUtils.findAvailableUsersWithEndDateTime(null, new Date(), 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(Messages.NULL_DATE_EXCEPTION);
    }

    @Test
    void findAvailableUsersWithEndDateTime_shouldThrowIllegalArgumentException_whenEndDateIsNull() {
        assertThatThrownBy(() -> orderUtils.findAvailableUsersWithEndDateTime(new Date(), null, 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(Messages.NULL_DATE_EXCEPTION);
    }

    @Test
    void findAvailableUsersWithEndDateTime_shouldReturnAvailableEmployee_whenNoOverlappingOrders() {
        Date start = new Date(1000);
        Date end = new Date(5000);

        when(userRepository.findUsersByRole_NameIn(anyCollection())).thenReturn(List.of(employeeUser));
        when(orderRepository.findBookedIntervalsForEmployees(
                List.of(1L), start, end)).thenReturn(List.of());

        List<User> result = orderUtils.findAvailableUsersWithEndDateTime(start, end, 60);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Anna Nowak");
    }

    @Test
    void findAvailableUsersWithEndDateTime_shouldNotReturnEmployee_whenOrderOverlaps() {
        Date taskStart = new Date(2000);
        Date taskEnd = new Date(8000);

        Order overlappingOrder = new Order();
        overlappingOrder.setStartDate(new Date(1000));
        overlappingOrder.setEndDate(new Date(9000));

        when(userRepository.findUsersByRole_NameIn(anyCollection())).thenReturn(List.of(employeeUser));
        when(orderRepository.findBookedIntervalsForEmployees(
                List.of(1L), taskStart, taskEnd)).thenReturn(List.of(
                new BookingInterval(1L, overlappingOrder.getStartDate(), overlappingOrder.getEndDate())));

        List<User> result = orderUtils.findAvailableUsersWithEndDateTime(taskStart, taskEnd, 60);

        assertThat(result).isEmpty();
    }

    @Test
    void findAvailableUsersWithEndDateTime_shouldSkipUsersWithoutRole() {
        User userWithoutRole = new User("Bez roli", "bezroli@test.pl", "haslo", true);
        userWithoutRole.setRole(null);

        when(userRepository.findUsersByRole_NameIn(anyCollection())).thenReturn(List.of(userWithoutRole));

        List<User> result = orderUtils.findAvailableUsersWithEndDateTime(new Date(1000), new Date(5000), 60);

        assertThat(result).isEmpty();
        verify(orderRepository, never()).findBookedIntervalsForEmployees(anyList(), any(), any());
    }

    // findNextAvailableSlot

    @Test
    void findNextAvailableSlot_shouldReturnAvailableSlot_whenUserIsAvailable() {
        Calendar monday = futureDayAt(Calendar.MONDAY, 8, 0);

        AvailabilityDTO result = orderUtils.findNextAvailableSlot(
                monday.getTime(),
                2.0,
                (current, end) -> List.of(employeeUser)
        );

        assertThat(result.available()).isTrue();
        assertThat(result.message()).isEqualTo(Messages.SLOT_AVAILABLE);
        assertThat(result.userName()).isEqualTo("Anna Nowak");
        assertThat(result.startDateTime()).startsWith(asDate(monday));
        assertThat(result.startDateTime()).endsWith("08:00:00");
        assertThat(result.endDateTime()).endsWith("10:00:00");
    }

    @Test
    void findNextAvailableSlot_shouldSearchLaterTimesOnTheSameDay() {
        Calendar monday = futureDayAt(Calendar.MONDAY, 8, 0);

        AvailabilityDTO result = orderUtils.findNextAvailableSlot(
                monday.getTime(),
                1.0,
                (start, end) -> start.get(Calendar.HOUR_OF_DAY) >= 9
                        ? List.of(employeeUser)
                        : List.of()
        );

        assertThat(result.available()).isTrue();
        assertThat(result.startDateTime()).startsWith(asDate(monday));
        assertThat(result.startDateTime()).endsWith("09:00:00");
        assertThat(result.endDateTime()).endsWith("10:00:00");
    }

    @Test
    void findNextAvailableSlot_shouldRoundStartTimeUpToTheNextHalfHour() {
        Calendar monday = futureDayAt(Calendar.MONDAY, 8, 10);

        AvailabilityDTO result = orderUtils.findNextAvailableSlot(
                monday.getTime(),
                1.0,
                (start, end) -> List.of(employeeUser)
        );

        assertThat(result.available()).isTrue();
        assertThat(result.startDateTime()).startsWith(asDate(monday));
        assertThat(result.startDateTime()).endsWith("08:30:00");
        assertThat(result.endDateTime()).endsWith("09:30:00");
    }

    @Test
    void findNextAvailableSlot_shouldMoveToNextWorkingDayWhenNoSlotFits() {
        Calendar monday = futureDayAt(Calendar.MONDAY, 15, 30);
        Calendar expectedTuesday = (Calendar) monday.clone();
        expectedTuesday.add(Calendar.DAY_OF_MONTH, 1);

        AvailabilityDTO result = orderUtils.findNextAvailableSlot(
                monday.getTime(),
                2.0,
                (start, end) -> List.of(employeeUser)
        );

        assertThat(result.available()).isTrue();
        assertThat(result.startDateTime()).startsWith(asDate(expectedTuesday));
        assertThat(result.startDateTime()).endsWith("08:00:00");
        assertThat(result.endDateTime()).endsWith("10:00:00");
    }

    @Test
    void findNextAvailableSlot_shouldReturnNotAvailable_whenNoUsersFound() {
        Calendar monday = futureDayAt(Calendar.MONDAY, 8, 0);

        AvailabilityDTO result = orderUtils.findNextAvailableSlot(
                monday.getTime(),
                2.0,
                (current, end) -> List.of()
        );

        assertThat(result.available()).isFalse();
        assertThat(result.message()).isEqualTo(Messages.NO_AVAILABLE_EMPLOYEES);
        assertThat(result.startDateTime()).isNull();
        assertThat(result.endDateTime()).isNull();
    }

    @Test
    void findNextAvailableSlot_shouldSkipWeekends_andFindNextMonday() {
        Calendar fridayEvening = futureDayAt(Calendar.FRIDAY, 17, 0);

        Calendar expectedMonday = (Calendar) fridayEvening.clone();
        expectedMonday.add(Calendar.DAY_OF_MONTH, 3);

        AvailabilityDTO result = orderUtils.findNextAvailableSlot(
                fridayEvening.getTime(),
                2.0,
                (current, end) -> current.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                        ? List.of(employeeUser)
                        : List.of()
        );

        assertThat(result.available()).isTrue();
        assertThat(result.startDateTime()).startsWith(asDate(expectedMonday));
        assertThat(result.startDateTime()).endsWith("08:00:00");
    }

    @Test
    void findNextAvailableSlot_shouldReturnNotAvailable_whenTaskExceedsWorkdayEnd() {
        Calendar monday = futureDayAt(Calendar.MONDAY, 8, 0);

        AvailabilityDTO result = orderUtils.findNextAvailableSlot(
                monday.getTime(),
                10.0,
                (current, end) -> List.of(employeeUser)
        );

        assertThat(result.available()).isFalse();
    }

    @Test
    void findNextAvailableSlot_shouldRejectTaskEndingAfterWorkdayEnd_evenWithinSameHour() {
        Calendar monday = futureDayAt(Calendar.MONDAY, 8, 0);

        AvailabilityDTO result = orderUtils.findNextAvailableSlot(
                monday.getTime(),
                8.5,
                (current, end) -> List.of(employeeUser)
        );

        assertThat(result.available()).isFalse();
    }

    @Test
    void findNextAvailableSlot_shouldNotProposeSlotInThePast() {
        Calendar longAgo = Calendar.getInstance();
        longAgo.add(Calendar.YEAR, -2);

        AvailabilityDTO result = orderUtils.findNextAvailableSlot(
                longAgo.getTime(),
                2.0,
                (current, end) -> List.of(employeeUser)
        );

        assertThat(result.available()).isTrue();
        assertThat(result.startDateTime().substring(0, 10))
                .isGreaterThanOrEqualTo(new SimpleDateFormat(DATE_ONLY).format(new Date()));
    }

    // findOrdersForAdmin / findOrdersForUser

    @Test
    void findOrdersForAdmin_shouldCallFindByEndDateBetween() {
        DateRange dateRange = new DateRange(new Date(1000), new Date(9000));
        when(orderRepository.findByEndDateBetween(dateRange.start(), dateRange.end()))
                .thenReturn(List.of(order));

        List<Order> result = orderUtils.findOrdersForAdmin(dateRange);

        assertThat(result).hasSize(1);
        verify(orderRepository).findByEndDateBetween(dateRange.start(), dateRange.end());
    }

    @Test
    void findOrdersForUser_shouldCallFindByEmployeeIdAndEndDateBetween() {
        DateRange dateRange = new DateRange(new Date(1000), new Date(9000));
        when(orderRepository.findByEmployeeIdAndEndDateBetween(1L, dateRange.start(), dateRange.end()))
                .thenReturn(List.of(order));

        List<Order> result = orderUtils.findOrdersForUser(1L, dateRange);

        assertThat(result).hasSize(1);
        verify(orderRepository).findByEmployeeIdAndEndDateBetween(1L, dateRange.start(), dateRange.end());
    }

    @Test
    void findAvailableUsersWithEndDateTime_shouldRejectIdenticalOccupiedSlot() {
        Date start = new Date(1000);
        Date end = new Date(3000);
        Order existing = new Order();
        existing.setStartDate(start);
        existing.setEndDate(end);

        when(userRepository.findUsersByRole_NameIn(anyCollection())).thenReturn(List.of(employeeUser));
        when(orderRepository.findBookedIntervalsForEmployees(
                List.of(1L), start, end)).thenReturn(List.of(
                new BookingInterval(1L, existing.getStartDate(), existing.getEndDate())));

        assertThat(orderUtils.findAvailableUsersWithEndDateTime(start, end, 60)).isEmpty();
    }

    @Test
    void findAvailableUsersWithEndDateTime_shouldAllowAdjacentSlots() {
        Date start = new Date(3000);
        Date end = new Date(5000);
        Order adjacent = new Order();
        adjacent.setStartDate(new Date(1000));
        adjacent.setEndDate(start);
        // Also verify interval logic directly, independently of repository filtering.
        when(userRepository.findUsersByRole_NameIn(anyCollection())).thenReturn(List.of(employeeUser));
        when(orderRepository.findBookedIntervalsForEmployees(
                List.of(1L), start, end)).thenReturn(List.of(
                new BookingInterval(1L, adjacent.getStartDate(), adjacent.getEndDate())));

        assertThat(orderUtils.findAvailableUsersWithEndDateTime(start, end, 60))
                .containsExactly(employeeUser);
    }

    @Test
    void toOrderDTO_shouldUseIsoLocalDateTimeInWarsaw() {
        order.setStartDate(java.util.Date.from(java.time.Instant.parse("2026-07-01T07:30:00Z")));
        order.setEndDate(java.util.Date.from(java.time.Instant.parse("2026-07-01T09:00:00Z")));

        OrderDTO result = orderUtils.toOrderDTO(order);

        assertThat(result.startDate()).isEqualTo("2026-07-01T09:30:00");
        assertThat(result.endDate()).isEqualTo("2026-07-01T11:00:00");
    }

    @Test
    void prepareAvailabilityLookup_shouldReusePrefetchedBookingsAcrossCandidateSlots() {
        Calendar monday = futureDayAt(Calendar.MONDAY, 8, 0);
        Date start = monday.getTime();
        Date busyEnd = new Date(start.getTime() + 60 * 60_000L);

        when(userRepository.findUsersByRole_NameIn(anyCollection()))
                .thenReturn(List.of(employeeUser));
        when(orderRepository.findBookedIntervalsForEmployees(eq(List.of(1L)), eq(start), any(Date.class)))
                .thenReturn(List.of(new BookingInterval(1L, start, busyEnd)));

        var lookup = orderUtils.prepareAvailabilityLookup(start, null, null);
        Calendar firstEnd = (Calendar) monday.clone();
        firstEnd.add(Calendar.HOUR_OF_DAY, 1);
        assertThat(lookup.apply(monday, firstEnd)).isEmpty();

        Calendar nextStart = (Calendar) monday.clone();
        nextStart.add(Calendar.HOUR_OF_DAY, 1);
        Calendar nextEnd = (Calendar) nextStart.clone();
        nextEnd.add(Calendar.HOUR_OF_DAY, 1);
        assertThat(lookup.apply(nextStart, nextEnd)).containsExactly(employeeUser);

        verify(userRepository, times(1)).findUsersByRole_NameIn(anyCollection());
        verify(orderRepository, times(1)).findBookedIntervalsForEmployees(eq(List.of(1L)), eq(start), any(Date.class));
        verifyNoMoreInteractions(orderRepository);
    }

}
