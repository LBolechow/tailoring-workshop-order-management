package pl.lukbol.dyplom.integrationTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.lukbol.dyplom.DTOs.order.AddOrderDTO;
import pl.lukbol.dyplom.DTOs.order.EditOrderDTO;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.BookingSlotService;
import pl.lukbol.dyplom.services.OrderService;
import pl.lukbol.dyplom.utilities.OrderUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Actual concurrency check against PostgreSQL. Test methods must NOT run inside
 * the default @DataJpaTest transaction: worker threads use their own committed
 * transactions and have to see the fixtures created in @BeforeEach.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OrderService.class, OrderUtils.class, BookingSlotService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
class BookingConcurrencyPostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("tailoring_integration")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDb(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired TransactionTemplate transactions;

    private String employeeName;
    private Long employeeId;

    @BeforeEach
    void prepareCommittedEmployee() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        employeeName = "Employee " + id;
        employeeId = transactions.execute(status -> {
            Role role = roleRepository.save(new Role("ROLE_EMPLOYEE"));
            User user = new User(employeeName, id + "@example.test", "hash", true);
            user.setRole(role);
            return userRepository.save(user).getId();
        });
    }

    private AddOrderDTO add(String start, String end) {
        return new AddOrderDTO(
                "Booking", "Customer", "customer@example.test", "123456789",
                1.0, start, end, employeeName, 100, "W trakcie", List.of());
    }

    @Test
    void twoConcurrentIdenticalBookingsPersistExactlyOne() throws Exception {
        // 09:00 Warsaw is 08:00 UTC in November.
        AddOrderDTO booking = add("2026-11-02T09:00:00", "2026-11-02T10:00:00");
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Object> job = () -> {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Workers failed to start");
                }
                try {
                    orderService.addOrder(booking);
                    return "created";
                } catch (ApplicationException.OrderSlotConflictException ex) {
                    return ex;
                }
            };
            Future<Object> first = executor.submit(job);
            Future<Object> second = executor.submit(job);
            start.countDown();

            List<Object> results = List.of(
                    first.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS));
            assertThat(results.stream().filter("created"::equals).count()).isEqualTo(1);
            assertThat(results.stream().filter(ApplicationException.OrderSlotConflictException.class::isInstance).count())
                    .isEqualTo(1);
            assertThat(orderRepository.findOverlappingOrdersForEmployee(
                    employeeId,
                    Date.from(Instant.parse("2026-11-02T08:00:00Z")),
                    Date.from(Instant.parse("2026-11-02T09:00:00Z"))))
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void adjacentBookingIsAllowedButOverlappingEditIsRejected() {
        orderService.addOrder(add("2026-11-02T09:00:00", "2026-11-02T10:00:00"));
        orderService.addOrder(add("2026-11-02T10:00:00", "2026-11-02T11:00:00"));

        Long secondOrderId = orderRepository.findOverlappingOrdersForEmployee(
                employeeId,
                Date.from(Instant.parse("2026-11-02T09:00:00Z")),
                Date.from(Instant.parse("2026-11-02T10:00:00Z"))).get(0).getId();

        EditOrderDTO sameTime = new EditOrderDTO(
                "Updated", "Customer", "customer@example.test", "123456789",
                1.0, "2026-11-02T10:00:00", "2026-11-02T11:00:00",
                employeeName, 100, "W trakcie", List.of());
        orderService.editOrder(secondOrderId, sameTime); // must not overlap itself

        EditOrderDTO conflict = new EditOrderDTO(
                "Updated", "Customer", "customer@example.test", "123456789",
                1.0, "2026-11-02T09:30:00", "2026-11-02T10:30:00",
                employeeName, 100, "W trakcie", List.of());
        assertThatThrownBy(() -> orderService.editOrder(secondOrderId, conflict))
                .isInstanceOf(ApplicationException.OrderSlotConflictException.class);
    }
}
