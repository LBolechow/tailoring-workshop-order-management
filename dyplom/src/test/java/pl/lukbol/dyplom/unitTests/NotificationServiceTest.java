package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import pl.lukbol.dyplom.DTOs.notification.NotificationRequestDTO;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.classes.Notification;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.NotificationRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.NotificationService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private User sender;
    private User participant;

    @BeforeEach
    void setUp() {
        user = new User("Jan Kowalski", "jan@test.pl", "haslo", false);
        user.setNotifications(new ArrayList<>(List.of(
                new Notification("Powiadomienie 1"),
                new Notification("Powiadomienie 2")
        )));

        sender = new User("Admin", "admin@test.pl", "haslo", true);

        participant = new User("Pracownik", "pracownik@test.pl", "haslo", true);
        participant.setNotifications(new ArrayList<>());
    }

    // removeAlerts

    @Test
    void removeAlerts_shouldClearNotificationsAndReturnSuccessMessage() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("jan@test.pl");
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(user);

        ApiResponseDTO result = notificationService.removeAlerts(authentication);

        assertThat(user.getNotifications()).isEmpty();
        verify(notificationRepository).deleteAllByUserId(user.getId());
        verify(userRepository).save(user);
        assertThat(result.message()).isEqualTo(Messages.ALERTS_REMOVED_MSG);
    }

    @Test
    void removeAlerts_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("nieistnieje@test.pl");
        when(userRepository.findByEmail("nieistnieje@test.pl")).thenReturn(null);

        assertThatThrownBy(() -> notificationService.removeAlerts(authentication))
                .isInstanceOf(ApplicationException.UserNotFoundException.class)
                .hasMessage(Messages.USER_NOT_FOUND_BY_EMAIL);

        verify(notificationRepository, never()).deleteAllByUserId(any());
        verify(userRepository, never()).save(any());
    }

    // createNotification

    @Test
    void createNotification_shouldCreateNotificationForEachParticipant() {
        NotificationRequestDTO dto = new NotificationRequestDTO("Nowe zadanie!", List.of(2L));
        when(userRepository.findByEmail("admin@test.pl")).thenReturn(sender);
        when(userRepository.findByIdIn(List.of(2L))).thenReturn(List.of(participant));

        ApiResponseDTO result = notificationService.createNotification("admin@test.pl", dto);

        assertThat(participant.getNotifications()).hasSize(1);
        assertThat(participant.getNotifications().get(0).getDescription()).isEqualTo("Nowe zadanie!");
        assertThat(participant.getNotifications().get(0).getCreator()).isEqualTo("Admin");
        verify(userRepository).save(participant);
        assertThat(result.message()).isEqualTo(Messages.NOTIFICATION_CREATED_SUCCESS_MSG);
    }

    @Test
    void createNotification_shouldCreateNotificationsForMultipleParticipants() {
        User participant2 = new User("Pracownik2", "pracownik2@test.pl", "haslo", true);
        participant2.setNotifications(new ArrayList<>());

        NotificationRequestDTO dto = new NotificationRequestDTO("Ważne info!", List.of(2L, 3L));
        when(userRepository.findByEmail("admin@test.pl")).thenReturn(sender);
        when(userRepository.findByIdIn(List.of(2L, 3L))).thenReturn(List.of(participant, participant2));

        notificationService.createNotification("admin@test.pl", dto);

        assertThat(participant.getNotifications()).hasSize(1);
        assertThat(participant2.getNotifications()).hasSize(1);
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void createNotification_shouldThrowParticipantsListIsEmptyException_whenNoParticipants() {
        NotificationRequestDTO dto = new NotificationRequestDTO("Test", List.of());
        when(userRepository.findByEmail("admin@test.pl")).thenReturn(sender);
        when(userRepository.findByIdIn(List.of())).thenReturn(List.of());

        assertThatThrownBy(() -> notificationService.createNotification("admin@test.pl", dto))
                .isInstanceOf(ApplicationException.ParticipantsListIsEmptyException.class)
                .hasMessage(Messages.PARTICIPANTS_LIST_IS_EMPTY);

        verify(userRepository, never()).save(any());
    }

    @Test
    void createNotification_shouldSetCorrectDateOnNotification() {
        NotificationRequestDTO dto = new NotificationRequestDTO("Testowe powiadomienie", List.of(2L));
        when(userRepository.findByEmail("admin@test.pl")).thenReturn(sender);
        when(userRepository.findByIdIn(List.of(2L))).thenReturn(List.of(participant));

        notificationService.createNotification("admin@test.pl", dto);

        Notification created = participant.getNotifications().get(0);
        assertThat(created.getDate()).isNotNull();
        assertThat(created.getUser()).isEqualTo(participant);
    }
}