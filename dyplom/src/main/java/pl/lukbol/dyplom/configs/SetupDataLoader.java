package pl.lukbol.dyplom.configs;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.common.UserRole;
import pl.lukbol.dyplom.classes.Privilege;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.PrivilegeRepository;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private static final String PRIVILEGE_READ = "READ_PRIVILEGE";
    private static final String PRIVILEGE_WRITE = "WRITE_PRIVILEGE";

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.password}")
    private String adminPassword;

    private boolean alreadySetup = false;

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (alreadySetup) return;

        Privilege readPrivilege = createPrivilegeIfNotFound(PRIVILEGE_READ);
        Privilege writePrivilege = createPrivilegeIfNotFound(PRIVILEGE_WRITE);

        createRoleIfNotFound(UserRole.ADMIN.authority(), List.of(readPrivilege, writePrivilege));
        createRoleIfNotFound(UserRole.EMPLOYEE.authority(), List.of(readPrivilege));
        createRoleIfNotFound(UserRole.CLIENT.authority(), List.of(readPrivilege));

        if (userRepository.findByEmail(adminEmail) == null) {
            Role adminRole = roleRepository.findByName(UserRole.ADMIN.authority());
            User adminUser = new User();
            adminUser.setName(adminName);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setEmail(adminEmail);
            adminUser.setEnabled(true);
            adminUser.setRole(adminRole);
            userRepository.save(adminUser);
            log.info("Utworzono konto administratora: {}", adminEmail);
        }

        alreadySetup = true;
        log.info("Inicjalizacja danych startowych zakonczona");
    }

    @Transactional
    Privilege createPrivilegeIfNotFound(String name) {
        Privilege privilege = privilegeRepository.findByName(name);
        if (privilege == null) {
            privilege = new Privilege(name);
            privilegeRepository.save(privilege);
        }
        return privilege;
    }

    @Transactional
    Role createRoleIfNotFound(String name, Collection<Privilege> privileges) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role(name);
            role.setPrivileges(privileges);
            roleRepository.save(role);
        }
        return role;
    }
}