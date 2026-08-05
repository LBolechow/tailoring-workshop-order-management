package pl.lukbol.dyplom.integrationTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Role roleAdmin;
    private Role roleEmployee;
    private Role roleClient;

    @BeforeEach
    void setUp() {
        roleAdmin = entityManager.persist(new Role("ROLE_ADMIN"));
        roleEmployee = entityManager.persist(new Role("ROLE_EMPLOYEE"));
        roleClient = entityManager.persist(new Role("ROLE_CLIENT"));

        persistUser("Admin Adminowski", "admin@test.pl", roleAdmin);
        persistUser("Anna Nowak", "anna@test.pl", roleEmployee);
        persistUser("Piotr Kowalski", "piotr@test.pl", roleEmployee);
        persistUser("Jan Klient", "jan@test.pl", roleClient);

        entityManager.flush();
        entityManager.clear();
    }

    private User persistUser(String name, String email, Role role) {
        User user = new User(name, email, "haslo", true);
        user.setRole(role);
        return entityManager.persist(user);
    }

    @Test
    void findAll_shouldReturnAllUsersWithRoleLoaded() {
        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(4);
        assertThat(users).allSatisfy(user ->
                assertThat(user.getRole()).isNotNull()
        );
    }

    @Test
    void findByEmail_shouldReturnUser() {
        User user = userRepository.findByEmail("anna@test.pl");

        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("Anna Nowak");
        assertThat(user.getRole().getName()).isEqualTo("ROLE_EMPLOYEE");
    }

    @Test
    void findByEmail_shouldReturnNull_whenUserDoesNotExist() {
        assertThat(userRepository.findByEmail("nieistnieje@test.pl")).isNull();
    }

    @Test
    void findOptionalByEmail_shouldReturnEmpty_whenUserDoesNotExist() {
        assertThat(userRepository.findOptionalByEmail("nieistnieje@test.pl")).isEmpty();
    }

    @Test
    void findByNameContainingIgnoreCase_shouldBeCaseInsensitive() {
        assertThat(userRepository.findByNameContainingIgnoreCase("anna")).hasSize(1);
        assertThat(userRepository.findByNameContainingIgnoreCase("ANNA")).hasSize(1);
        assertThat(userRepository.findByNameContainingIgnoreCase("kowalski")).hasSize(1);
    }

    @Test
    void findByEmailContainingIgnoreCase_shouldMatchPartialEmail() {
        assertThat(userRepository.findByEmailContainingIgnoreCase("test.pl")).hasSize(4);
        assertThat(userRepository.findByEmailContainingIgnoreCase("ANNA@")).hasSize(1);
    }

    @Test
    void findByRole_NameContainingIgnoreCase_shouldReturnUsersWithGivenRole() {
        List<User> employees = userRepository.findByRole_NameContainingIgnoreCase("ROLE_EMPLOYEE");

        assertThat(employees).hasSize(2);
        assertThat(employees).extracting(User::getName)
                .containsExactlyInAnyOrder("Anna Nowak", "Piotr Kowalski");
    }

    @Test
    void findUsersByRole_NameIn_shouldReturnUsersFromMultipleRoles() {
        List<User> staff = userRepository.findUsersByRole_NameIn(
                List.of("ROLE_EMPLOYEE", "ROLE_ADMIN")
        );

        assertThat(staff).hasSize(3);
        assertThat(staff).extracting(User::getName)
                .doesNotContain("Jan Klient");
    }

    @Test
    void findUsersByRole_NameIn_shouldReturnEmptyList_whenNoMatchingRole() {
        assertThat(userRepository.findUsersByRole_NameIn(List.of("ROLE_NIEISTNIEJACA"))).isEmpty();
    }

    @Test
    void findAllByNameNot_shouldExcludeGivenName() {
        List<User> users = userRepository.findAllByNameNot("Anna Nowak");

        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getName).doesNotContain("Anna Nowak");
    }

    @Test
    void findByName_shouldReturnExactMatch() {
        User user = userRepository.findByName("Piotr Kowalski");

        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("piotr@test.pl");
    }

    @Test
    void findByIdIn_shouldReturnUsersWithGivenIds() {
        List<Long> ids = userRepository.findAll().stream()
                .map(User::getId)
                .limit(2)
                .toList();

        assertThat(userRepository.findByIdIn(ids)).hasSize(2);
    }
}