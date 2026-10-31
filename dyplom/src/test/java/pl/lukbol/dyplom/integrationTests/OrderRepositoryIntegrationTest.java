package pl.lukbol.dyplom.integrationTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.OrderRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryIntegrationTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private OrderRepository orderRepository;

    private User anna;
    private User otherAnna;

    @BeforeEach
    void setUp() {
        Role employeeRole = entityManager.persist(new Role("ROLE_EMPLOYEE"));
        anna = new User("Anna Nowak", "anna.one@test.pl", "hash", true);
        anna.setRole(employeeRole);
        anna = entityManager.persist(anna);

        // Same display name, different employee ID. Bookings must not be mixed.
        otherAnna = new User("Anna Nowak", "anna.two@test.pl", "hash", true);
        otherAnna.setRole(employeeRole);
        otherAnna = entityManager.persist(otherAnna);

        entityManager.persist(new Order(
                "Zamówienie", "Klient", "client@test.pl", "123",
                anna, at("2026-11-02T09:00:00Z"), at("2026-11-02T10:00:00Z"),
                "W trakcie", 100, 1.0, new ArrayList<>(), "ORDER-001"));
        entityManager.flush();
        entityManager.clear();
    }

    private static Date at(String isoInstant) {
        return Date.from(Instant.parse(isoInstant));
    }

    @Test
    void overlappingOrders_shouldBeFoundByEmployeeIdNotName() {
        assertThat(orderRepository.findOverlappingOrdersForEmployee(
                anna.getId(), at("2026-11-02T09:30:00Z"), at("2026-11-02T10:30:00Z")))
                .hasSize(1);
        assertThat(orderRepository.findOverlappingOrdersForEmployee(
                otherAnna.getId(), at("2026-11-02T09:30:00Z"), at("2026-11-02T10:30:00Z")))
                .isEmpty();
    }

    @Test
    void adjacentSlots_shouldNotOverlap() {
        assertThat(orderRepository.findOverlappingOrdersForEmployee(
                anna.getId(), at("2026-11-02T10:00:00Z"), at("2026-11-02T11:00:00Z")))
                .isEmpty();
    }

    @Test
    void employeeRename_shouldBeReflectedInExistingOrder() {
        User employee = entityManager.find(User.class, anna.getId());
        employee.setName("Anna Kowalska");
        entityManager.flush();
        entityManager.clear();

        Order saved = orderRepository.findOverlappingOrdersForEmployee(
                anna.getId(), at("2026-11-02T09:00:00Z"), at("2026-11-02T10:00:00Z")).get(0);
        assertThat(saved.getEmployeeName()).isEqualTo("Anna Kowalska");
    }
}
