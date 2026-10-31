package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.services.OrderAccessService;

import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAccessServiceTest {
    @Mock OrderRepository orders;
    @Mock MaterialRepository materials;
    @InjectMocks OrderAccessService access;

    private final pl.lukbol.dyplom.classes.User employee =
            new pl.lukbol.dyplom.classes.User("Worker", "worker@example.test", "hash", true);
    private final Order order = new Order();

    private Authentication auth(String email, String role) {
        var principal = new User(email, "hash", List.of(new SimpleGrantedAuthority(role)));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @BeforeEach void fixture() {
        employee.setId(10L);
        order.setEmployee(employee);
    }

    @Test void ownerEmployeeCanReadOwnOrder() {
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        assertThatCode(() -> access.assertCanAccessOrder(7L, auth("worker@example.test", "ROLE_EMPLOYEE")))
                .doesNotThrowAnyException();
    }
    @Test void otherEmployeeCannotReadOrder() {
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> access.assertCanAccessOrder(7L, auth("other@example.test", "ROLE_EMPLOYEE")))
                .isInstanceOf(ApplicationException.OrderAccessDeniedException.class);
    }
    @Test void clientCannotReadEmployeesOrderEvenIfEmailMatches() {
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> access.assertCanAccessOrder(7L, auth("worker@example.test", "ROLE_CLIENT")))
                .isInstanceOf(ApplicationException.OrderAccessDeniedException.class);
    }
    @Test void administratorCanReadAnyOrder() {
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        assertThatCode(() -> access.assertCanAccessOrder(7L, auth("admin@example.test", "ROLE_ADMIN")))
                .doesNotThrowAnyException();
    }
    @Test void employeeCannotAssignWorkToOtherEmployee() {
        var other = new pl.lukbol.dyplom.classes.User("Other", "other@example.test", "hash", true);
        other.setRole(new pl.lukbol.dyplom.classes.Role("ROLE_EMPLOYEE"));
        var lookup = mock(pl.lukbol.dyplom.utilities.OrderUtils.class);
        when(lookup.findUserByName("Other")).thenReturn(other);
        assertThatThrownBy(() -> access.assertCanAssignEmployee(
                "Other", auth("worker@example.test", "ROLE_EMPLOYEE"), lookup))
                .isInstanceOf(ApplicationException.OrderAccessDeniedException.class);
    }
    @Test void employeeCannotUpdateOtherEmployeeMaterial() {
        var material = new pl.lukbol.dyplom.classes.Material("fabric", order, false);
        when(materials.findById(5L)).thenReturn(Optional.of(material));
        assertThatThrownBy(() -> access.assertCanAccessMaterial(5L,
                auth("other@example.test", "ROLE_EMPLOYEE")))
                .isInstanceOf(ApplicationException.OrderAccessDeniedException.class);
    }
    @Test void unknownOrderReturnsNotFound() {
        when(orders.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> access.assertCanAccessOrder(999L, auth("admin@example.test", "ROLE_ADMIN")))
                .isInstanceOf(ApplicationException.OrderNotFoundException.class);
    }
}
