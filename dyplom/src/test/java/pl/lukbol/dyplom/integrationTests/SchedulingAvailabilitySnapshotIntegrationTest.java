package pl.lukbol.dyplom.integrationTests;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.utilities.OrderUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Import(OrderUtils.class)
class SchedulingAvailabilitySnapshotIntegrationTest {

    // The legacy slot iterator uses Calendar.getInstance() (system zone).
    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired OrderUtils orderUtils;

    private static Date at(LocalDate day, int hour) {
        return Date.from(day.atTime(LocalTime.of(hour, 0)).atZone(ZONE).toInstant());
    }

    private static Calendar calendar(Date value) {
        Calendar result = Calendar.getInstance(TimeZone.getTimeZone(ZONE));
        result.setTime(value);
        return result;
    }

    @Test
    void loadsBusyIntervalsOnceAndChecksManySlotsWithoutAdditionalSql() {
        // Use a future Monday: findNextAvailableSlot deliberately skips past slots.
        LocalDate monday = LocalDate.now(ZONE).plusWeeks(2)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        Role employeeRole = entityManager.merge(new Role("ROLE_EMPLOYEE"));
        Role adminRole = entityManager.merge(new Role("ROLE_ADMIN"));
        Role clientRole = entityManager.merge(new Role("ROLE_USER"));

        User anna = new User("Anna", "anna-snapshot@test.pl", "hash", true);
        anna.setRole(employeeRole);
        anna = entityManager.merge(anna);

        User bartek = new User("Bartek", "bartek-snapshot@test.pl", "hash", true);
        bartek.setRole(adminRole);
        bartek = entityManager.merge(bartek);

        User client = new User("Client", "client-snapshot@test.pl", "hash", true);
        client.setRole(clientRole);
        entityManager.merge(client);

        // Both employees are busy 08:00-09:00. Only Anna is busy 09:00-10:00.
        entityManager.persist(new Order("A", "Customer", "a@customer.pl", "123",
                anna, at(monday, 8), at(monday, 10), "W trakcie", 100, 2,
                new ArrayList<>(), "A-SNAPSHOT"));
        entityManager.persist(new Order("B", "Customer", "b@customer.pl", "123",
                bartek, at(monday, 8), at(monday, 9), "W trakcie", 100, 1,
                new ArrayList<>(), "B-SNAPSHOT"));
        entityManager.flush();
        entityManager.clear();

        var stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        Date start = at(monday, 8);
        var lookup = orderUtils.prepareAvailabilityLookup(start, null, null);
        long statementsAfterLoading = stats.getPrepareStatementCount();
        // One query for eligible staff + one for their intervals; no N+1.
        assertThat(statementsAfterLoading).isBetween(1L, 3L);

        for (int i = 0; i < 12; i++) {
            Calendar candidate = calendar(start);
            candidate.add(Calendar.MINUTE, i * 30);
            Calendar candidateEnd = (Calendar) candidate.clone();
            candidateEnd.add(Calendar.HOUR_OF_DAY, 1);
            lookup.apply(candidate, candidateEnd);
        }
        assertThat(stats.getPrepareStatementCount()).isEqualTo(statementsAfterLoading);

        // Both busy at 8:00, Bartek free at 9:00, user with ROLE_USER excluded.
        var suggested = orderUtils.findNextAvailableSlot(start, 1.0, lookup);
        assertThat(suggested.available()).isTrue();
        assertThat(suggested.userName()).isEqualTo("Bartek");
        assertThat(suggested.startDateTime()).endsWith("09:00:00");
        assertThat(stats.getPrepareStatementCount()).isEqualTo(statementsAfterLoading);

        var onlyAnna = orderUtils.prepareAvailabilityLookup(start, anna.getId(), null);
        var annaSuggestion = orderUtils.findNextAvailableSlot(start, 1.0, onlyAnna);
        assertThat(annaSuggestion.userName()).isEqualTo("Anna");
        assertThat(annaSuggestion.startDateTime()).endsWith("10:00:00");

        var excludeAnna = orderUtils.prepareAvailabilityLookup(start, null, anna.getId());
        var otherSuggestion = orderUtils.findNextAvailableSlot(start, 1.0, excludeAnna);
        assertThat(otherSuggestion.userName()).isEqualTo("Bartek");
        assertThat(otherSuggestion.startDateTime()).endsWith("09:00:00");
    }
}
