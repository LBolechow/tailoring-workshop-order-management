package pl.lukbol.dyplom.services;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.classes.PasswordResetToken;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.common.UserRole;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.DTOs.user.*;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.PasswordResetTokenRepository;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;
import pl.lukbol.dyplom.utilities.GenerateCode;
import pl.lukbol.dyplom.utilities.ResetTokenHasher;
import pl.lukbol.dyplom.utilities.UserUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserUtils userUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MessageRepository messageRepository;
    private final OrderRepository orderRepository;

    private final PasswordResetTokenRepository resetTokenRepository;
    private final ResetTokenHasher resetTokenHasher;

    @Transactional
    public ApiResponseDTO addUser(AddUserDTO addUserDTO) {
        if (userUtils.emailExists(addUserDTO.email())) {
            throw new ApplicationException.UserWithEmailAlreadyExistsException(Messages.EMAIL_ADDRES_ALREADY_EXIST);
        }

        User newUser = userUtils.createUser(addUserDTO.name(), addUserDTO.email(), addUserDTO.password());
        Role role = roleRepository.findByName(addUserDTO.role());
        newUser.setRole(role);
        userUtils.addWelcomeNotification(newUser);
        userRepository.save(newUser);

        return new ApiResponseDTO(Messages.USER_ADD_SUCCESS);
    }

    public List<UserDTO> getUsersByRoles() {
        List<User> users = new ArrayList<>();
        users.addAll(userRepository.findByRole_NameContainingIgnoreCase(UserRole.ADMIN.authority()));
        users.addAll(userRepository.findByRole_NameContainingIgnoreCase(UserRole.EMPLOYEE.authority()));
        return users.stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.isEnabled(),
                        user.getRole() != null ? user.getRole().getName() : null
                ))
                .toList();
    }


    @Transactional
    public ApiResponseDTO registerUser(RegisterRequestDTO registerRequestDTO) {
        if (userUtils.emailExists(registerRequestDTO.email())) {
            throw new ApplicationException.UserWithEmailAlreadyExistsException(Messages.EMAIL_ADDRES_ALREADY_EXIST);
        }
        User newUser = userUtils.createUser(registerRequestDTO.name(), registerRequestDTO.email(), registerRequestDTO.password());
        Role clientRole = roleRepository.findByName(UserRole.CLIENT.authority());
        newUser.setRole(clientRole);
        userUtils.addWelcomeNotification(newUser);
        userRepository.save(newUser);
        log.info("Zarejestrowano nowego uzytkownika: {}", registerRequestDTO.email());

        return new ApiResponseDTO(Messages.ACCOUNT_CREATED);
    }

    public UserDTO getUserByEmail(Authentication authentication) {
        String email = AuthenticationUtils.checkmail(authentication.getPrincipal());
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException(Messages.USER_NOT_FOUND_BY_EMAIL);
        }

        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isEnabled(),
                user.getRole() != null ? user.getRole().getName() : null
        );
    }

    @Transactional
    public ApiResponseDTO changeProfile(Authentication authentication, UpdateProfileRequestDTO updateProfileRequestDTO) {
        if (!updateProfileRequestDTO.password().equals(updateProfileRequestDTO.repeatPassword())) {
            throw new ApplicationException.PasswordsMismatchException(Messages.PASSWORDS_DO_NOT_MATCH);
        }
        String email = AuthenticationUtils.checkmail(authentication.getPrincipal());
        User user = userRepository.findByEmail(email);
        user.setPassword(passwordEncoder.encode(updateProfileRequestDTO.password()));
        user.setName(updateProfileRequestDTO.username());
        user.setEnabled(true);

        userRepository.save(user);
        return new ApiResponseDTO(Messages.PROFILE_UPDATED);
    }

    @Transactional
    public ApiResponseDTO deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApplicationException.UserNotFoundException(Messages.USER_NOT_FOUND_BY_ID));

        if (orderRepository.existsByEmployee_Id(id)) {
            throw new ApplicationException.UserHasOrdersException(
                    Messages.CANNOT_DELETE_EMPLOYEE_WITH_ORDERS);
        }

        messageRepository.deleteBySenderId(id);

        userUtils.removeClientFromConversations(id);
        userUtils.removeUserFromConversations(user);

        userRepository.delete(user);
        log.info("Usunieto konto uzytkownika id={}", id);

        return new ApiResponseDTO(Messages.ACCOUNT_DELETED);

    }

    public List<UserDTO> getEmployeesAndAdmins(Authentication authentication) {
        String email = AuthenticationUtils.checkmail(authentication.getPrincipal());
        User currentUser = userRepository.findByEmail(email);
        List<User> users = userRepository.findUsersByRole_NameIn(List.of(UserRole.EMPLOYEE.authority(), UserRole.ADMIN.authority()));
        users.removeIf(user -> user.getEmail().equalsIgnoreCase(currentUser.getEmail()));

        return users.stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.isEnabled(),
                        user.getRole().getName()
                ))
                .toList();
    }

    @Transactional
    public ApiResponseDTO updateUser(Long id, UpdateUserRequest updateUserRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApplicationException.UserNotFoundException(Messages.USER_NOT_FOUND_BY_ID));

        user.setName(updateUserRequest.name());
        user.setEmail(updateUserRequest.email());

        Role role = roleRepository.findByName(updateUserRequest.role());
        if (role == null) {
            throw new IllegalArgumentException(Messages.ROLE_NOT_FOUND + updateUserRequest.role());
        }
        user.setRole(role);

        userRepository.save(user);

        return new ApiResponseDTO(Messages.PROFILE_UPDATED);
    }

    public List<UserDTO> searchUsers(String category, String searchText) {
        List<User> users;

        switch (category.toLowerCase()) {
            case "name" -> users = userRepository.findByNameContainingIgnoreCase(searchText);
            case "email" -> users = userRepository.findByEmailContainingIgnoreCase(searchText);
            case "role" -> users = userRepository.findByRole_NameContainingIgnoreCase(searchText);
            default -> users = Collections.emptyList();
        }

        return users.stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.isEnabled(),
                        user.getRole().getName()
                ))
                .toList();
    }


    public List<String> getEmployeeNames() {
        List<User> users = userRepository.findUsersByRole_NameIn(List.of(UserRole.EMPLOYEE.authority(), UserRole.ADMIN.authority()));
        return users.stream()
                .map(User::getName)
                .distinct()
                .collect(Collectors.toList());
    }


    @Transactional
    public ApiResponseDTO forgotPassword(String email) {

        String responseMessage = Messages.RESET_MAIL_GENERIC;

        User user = userRepository.findByEmail(email);

        if (user == null) {
            return new ApiResponseDTO(responseMessage);
        }

        resetTokenRepository.deleteByUser_Id(user.getId());

        String token = GenerateCode.generateResetToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(resetTokenHasher.hash(token));
        resetToken.setExpiresAt(
                Instant.now().plus(15, ChronoUnit.MINUTES)
        );

        resetTokenRepository.save(resetToken);

        userUtils.sendPasswordResetEmail(email, token);

        return new ApiResponseDTO(responseMessage);
    }

    @Transactional
    public ApiResponseDTO resetPassword(
            String token,
            String newPassword) {

        String tokenHash = resetTokenHasher.hash(token);

        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(
                        ApplicationException.InvalidResetTokenException::new
                );

        if (!Instant.now().isBefore(resetToken.getExpiresAt())) {
            throw new ApplicationException.InvalidResetTokenException();
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);
        resetTokenRepository.deleteByUser_Id(user.getId());

        return new ApiResponseDTO(
                Messages.RESET_PASSWORD_SUCCESS
        );
    }



}