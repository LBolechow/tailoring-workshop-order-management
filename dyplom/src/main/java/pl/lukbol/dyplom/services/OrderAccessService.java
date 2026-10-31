package pl.lukbol.dyplom.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.common.UserRole;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;

import java.util.Objects;

/** Object-level authorization for the employee/admin order endpoints. */
@Service
@RequiredArgsConstructor
public class OrderAccessService {
    private final OrderRepository orders;
    private final MaterialRepository materials;

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> UserRole.ADMIN.authority().equals(a.getAuthority()));
    }

    private String email(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApplicationException.OrderAccessDeniedException(Messages.INVALID_AUTHENTICATION);
        }
        return AuthenticationUtils.checkmail(auth.getPrincipal());
    }

    private void assertEmployeeOrAdmin(Authentication auth, User employee) {
        if (isAdmin(auth)) return;
        String requester = email(auth);
        boolean employeeRole = auth.getAuthorities().stream()
                .anyMatch(a -> UserRole.EMPLOYEE.authority().equals(a.getAuthority()));
        if (!employeeRole || employee == null || !Objects.equals(employee.getEmail(), requester)) {
            throw new ApplicationException.OrderAccessDeniedException(Messages.ORDER_ACCESS_DENIED);
        }
    }

    @Transactional(readOnly = true)
    public void assertCanAccessOrder(Long id, Authentication auth) {
        Order order = orders.findById(id)
                .orElseThrow(() -> new ApplicationException.OrderNotFoundException(Messages.ORDER_NOT_FOUND));
        assertEmployeeOrAdmin(auth, order.getEmployee());
    }

    @Transactional(readOnly = true)
    public void assertCanAccessMaterial(Long materialId, Authentication auth) {
        var material = materials.findById(materialId)
                .orElseThrow(() -> new ApplicationException.MaterialNotFoundException(Messages.MATERIAL_NOT_FOUND));
        if (material.getOrder() == null) {
            throw new ApplicationException.OrderAccessDeniedException(Messages.ORDER_ACCESS_DENIED);
        }
        assertEmployeeOrAdmin(auth, material.getOrder().getEmployee());
    }

    /** An employee can manage own bookings, not assign work to another employee. */
    @Transactional(readOnly = true)
    public void assertCanAssignEmployee(String employeeName, Authentication auth,
                                        pl.lukbol.dyplom.utilities.OrderUtils orderUtils) {
        if (isAdmin(auth)) return;
        User target = orderUtils.findUserByName(employeeName);
        if (target.getRole() == null || !java.util.Set.of(
                UserRole.EMPLOYEE.authority(), UserRole.ADMIN.authority()).contains(target.getRole().getName())) {
            throw new ApplicationException.OrderAccessDeniedException(Messages.ORDER_ACCESS_DENIED);
        }
        assertEmployeeOrAdmin(auth, target);
    }
}
