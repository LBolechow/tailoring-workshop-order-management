package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.DTOs.user.*;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.UserService;
import pl.lukbol.dyplom.utilities.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserUtils userUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role roleClient;
    private Role roleAdmin;

    @BeforeEach
    void setUp() {
        roleClient = new Role("ROLE_CLIENT");
        roleAdmin = new Role("ROLE_ADMIN");

        user = new User("Jan Kowalski", "jan@test.pl", "encoded_haslo", false);
        user.setId(1L);
        user.setRole(roleClient);
        user.setNotifications(new ArrayList<>());
    }

    // addUser

    @Test
    void addUser_shouldCreateUserAndReturnSuccessMessage() {
        AddUserDTO dto = new AddUserDTO("Jan Kowalski", "jan@test.pl", "haslo", "ROLE_CLIENT");
        when(userUtils.emailExists(dto.email())).thenReturn(false);
        when(userUtils.createUser(dto.name(), dto.email(), dto.password())).thenReturn(user);
        when(roleRepository.findByName("ROLE_CLIENT")).thenReturn(roleClient);

        ApiResponseDTO result = userService.addUser(dto);

        verify(userUtils).addWelcomeNotification(user);
        verify(userRepository).save(user);
        assertThat(result.message()).isEqualTo(Messages.USER_ADD_SUCCESS);
    }

    @Test
    void addUser_shouldThrowException_whenEmailAlreadyExists() {
        AddUserDTO dto = new AddUserDTO("Jan Kowalski", "jan@test.pl", "haslo", "ROLE_CLIENT");
        when(userUtils.emailExists(dto.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.addUser(dto))
                .isInstanceOf(ApplicationException.UserWithEmailAlreadyExistsException.class)
                .hasMessage(Messages.EMAIL_ADDRES_ALREADY_EXIST);

        verify(userRepository, never()).save(any());
    }

    // registerUser

    @Test
    void registerUser_shouldCreateClientUserAndReturnSuccessMessage() {
        RegisterRequestDTO dto = new RegisterRequestDTO("Jan Kowalski", "jan@test.pl", "haslo");
        when(userUtils.emailExists(dto.email())).thenReturn(false);
        when(userUtils.createUser(dto.name(), dto.email(), dto.password())).thenReturn(user);
        when(roleRepository.findByName("ROLE_CLIENT")).thenReturn(roleClient);

        ApiResponseDTO result = userService.registerUser(dto);

        verify(userUtils).addWelcomeNotification(user);
        verify(userRepository).save(user);
        assertThat(result.message()).isEqualTo(Messages.ACCOUNT_CREATED);
        assertThat(user.getRole()).isEqualTo(roleClient);
    }

    @Test
    void registerUser_shouldThrowException_whenEmailAlreadyExists() {
        RegisterRequestDTO dto = new RegisterRequestDTO("Jan Kowalski", "jan@test.pl", "haslo");
        when(userUtils.emailExists(dto.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(ApplicationException.UserWithEmailAlreadyExistsException.class)
                .hasMessage(Messages.EMAIL_ADDRES_ALREADY_EXIST);

        verify(userRepository, never()).save(any());
    }

    // getUserByEmail

    @Test
    void getUserByEmail_shouldReturnUserDTO() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("jan@test.pl");
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(user);

        UserDTO result = userService.getUserByEmail(authentication);

        assertThat(result.email()).isEqualTo("jan@test.pl");
        assertThat(result.name()).isEqualTo("Jan Kowalski");
        assertThat(result.role()).isEqualTo("ROLE_CLIENT");
    }

    @Test
    void getUserByEmail_shouldThrowUsernameNotFoundException_whenUserNotFound() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("nieistnieje@test.pl");
        when(userRepository.findByEmail("nieistnieje@test.pl")).thenReturn(null);

        assertThatThrownBy(() -> userService.getUserByEmail(authentication))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(Messages.USER_NOT_FOUND_BY_EMAIL);
    }

    // changeProfile

    @Test
    void changeProfile_shouldUpdateUserAndReturnSuccessMessage() {
        UpdateProfileRequestDTO dto = new UpdateProfileRequestDTO("Nowe Imie", "nowehaslo", "nowehaslo");
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("jan@test.pl");
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(user);
        when(passwordEncoder.encode("nowehaslo")).thenReturn("encoded_nowehaslo");

        ApiResponseDTO result = userService.changeProfile(authentication, dto);

        assertThat(user.getName()).isEqualTo("Nowe Imie");
        assertThat(user.getPassword()).isEqualTo("encoded_nowehaslo");
        assertThat(user.isEnabled()).isTrue();
        verify(userRepository).save(user);
        assertThat(result.message()).isEqualTo(Messages.PROFILE_UPDATED);
    }

    @Test
    void changeProfile_shouldThrowPasswordsMismatchException_whenPasswordsDontMatch() {
        UpdateProfileRequestDTO dto = new UpdateProfileRequestDTO("Nowe Imie", "haslo1", "haslo2");

        assertThatThrownBy(() -> userService.changeProfile(authentication, dto))
                .isInstanceOf(ApplicationException.PasswordsMismatchException.class)
                .hasMessage(Messages.PASSWORDS_DO_NOT_MATCH);

        verify(userRepository, never()).save(any());
    }

    // deleteUser

    @Test
    void deleteUser_shouldDeleteUserAndReturnSuccessMessage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApiResponseDTO result = userService.deleteUser(1L);

        verify(messageRepository).deleteBySenderId(1L);
        verify(userUtils).removeClientFromConversations(1L);
        verify(userUtils).removeUserFromConversations(user);
        verify(userRepository).delete(user);
        assertThat(result.message()).isEqualTo(Messages.ACCOUNT_DELETED);
    }

    @Test
    void deleteUser_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ApplicationException.UserNotFoundException.class)
                .hasMessage(Messages.USER_NOT_FOUND_BY_ID);

        verify(userRepository, never()).delete(any());
    }

    // updateUser

    @Test
    void updateUser_shouldUpdateUserAndReturnSuccessMessage() {
        UpdateUserRequest request = new UpdateUserRequest("Nowe Imie", "nowy@test.pl", "ROLE_ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(roleAdmin);

        ApiResponseDTO result = userService.updateUser(1L, request);

        assertThat(user.getName()).isEqualTo("Nowe Imie");
        assertThat(user.getEmail()).isEqualTo("nowy@test.pl");
        assertThat(user.getRole()).isEqualTo(roleAdmin);
        verify(userRepository).save(user);
        assertThat(result.message()).isEqualTo(Messages.PROFILE_UPDATED);
    }

    @Test
    void updateUser_shouldThrowUserNotFoundException_whenUserNotFound() {
        UpdateUserRequest request = new UpdateUserRequest("Nowe Imie", "nowy@test.pl", "ROLE_ADMIN");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, request))
                .isInstanceOf(ApplicationException.UserNotFoundException.class)
                .hasMessage(Messages.USER_NOT_FOUND_BY_ID);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentException_whenRoleNotFound() {
        UpdateUserRequest request = new UpdateUserRequest("Nowe Imie", "nowy@test.pl", "ROLE_NIEZNANA");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_NIEZNANA")).thenReturn(null);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Messages.ROLE_NOT_FOUND);

        verify(userRepository, never()).save(any());
    }

    // searchUsers

    @Test
    void searchUsers_shouldReturnUsers_whenSearchByName() {
        when(userRepository.findByNameContainingIgnoreCase("Jan")).thenReturn(List.of(user));

        List<UserDTO> result = userService.searchUsers("name", "Jan");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Jan Kowalski");
    }

    @Test
    void searchUsers_shouldReturnUsers_whenSearchByEmail() {
        when(userRepository.findByEmailContainingIgnoreCase("jan")).thenReturn(List.of(user));

        List<UserDTO> result = userService.searchUsers("email", "jan");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("jan@test.pl");
    }

    @Test
    void searchUsers_shouldReturnEmptyList_whenCategoryUnknown() {
        List<UserDTO> result = userService.searchUsers("nieznana", "cos");

        assertThat(result).isEmpty();
        verifyNoInteractions(userRepository);
    }

    // getEmployeeNames

    @Test
    void getEmployeeNames_shouldReturnDistinctNames() {
        User employee1 = new User("Anna Nowak", "anna@test.pl", "haslo", true);
        User employee2 = new User("Anna Nowak", "anna2@test.pl", "haslo", true);
        User admin = new User("Admin", "admin@test.pl", "haslo", true);

        when(userRepository.findUsersByRole_NameIn(List.of("ROLE_EMPLOYEE", "ROLE_ADMIN")))
                .thenReturn(List.of(employee1, employee2, admin));

        List<String> result = userService.getEmployeeNames();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("Anna Nowak", "Admin");
    }

    // sendNewPassword

    @Test
    void sendNewPassword_shouldResetPasswordAndSendEmail() {
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("encoded_new_password");

        ApiResponseDTO result = userService.sendNewPassword(java.util.Map.of("email", "jan@test.pl"));

        verify(userRepository).save(user);
        verify(userUtils).sendResetEmail(eq("jan@test.pl"), any());
        assertThat(result.message()).isEqualTo(Messages.RESET_PASSWORD_LINK_SENT);
    }

    @Test
    void sendNewPassword_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findByEmail("nieistnieje@test.pl")).thenReturn(null);

        assertThatThrownBy(() -> userService.sendNewPassword(java.util.Map.of("email", "nieistnieje@test.pl")))
                .isInstanceOf(ApplicationException.UserNotFoundException.class)
                .hasMessage(Messages.USER_NOT_FOUND_BY_EMAIL);

        verify(userUtils, never()).sendResetEmail(any(), any());
    }
}