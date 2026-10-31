package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.lukbol.dyplom.classes.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /** Serializes scheduling writes for the same employee inside a transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :employeeId")
    Optional<User> findByIdForScheduling(@Param("employeeId") Long employeeId);

    User findByEmail(String email);


    Optional<User> findOptionalByEmail(String email);


    @EntityGraph(attributePaths = "role")
    List<User> findAll();

    List<User> findByNameContainingIgnoreCase(String name);


    List<User> findByEmailContainingIgnoreCase(String email);

    List<User> findByRole_NameContainingIgnoreCase(String roleName);


    User findByName(String name);

    // Multiple results are intentional: display names are not guaranteed unique.
    List<User> findByNameIgnoreCase(String name);

    List<User> findAllByNameNot(String employeeNameOnOrder);

    @EntityGraph(attributePaths = "role")
    List<User> findUsersByRole_NameIn(Collection<String> roleNames);
    List<User> findByIdIn(List<Long> ids);
}