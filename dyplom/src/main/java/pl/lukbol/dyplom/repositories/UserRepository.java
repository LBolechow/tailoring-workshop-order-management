package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.lukbol.dyplom.classes.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);


    Optional<User> findOptionalByEmail(String email);


    @EntityGraph(attributePaths = "roles")
    List<User> findAll();

    List<User> findByNameContainingIgnoreCase(String name);


    List<User> findByEmailContainingIgnoreCase(String email);

    List<User> findByRole_NameContainingIgnoreCase(String roleName);


    User findByName(String name);

    List<User> findAllByNameNot(String employeeNameOnOrder);

    List<User> findUsersByRole_NameIn(Collection<String> roleNames);
    List<User> findByIdIn(List<Long> ids);
}