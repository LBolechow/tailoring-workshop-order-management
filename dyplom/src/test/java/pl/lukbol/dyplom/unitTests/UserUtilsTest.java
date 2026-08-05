package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.UserUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUtilsTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserUtils userUtils;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Jan Kowalski", "jan@test.pl", "haslo", true);
        user.setId(1L);
        user.setNotifications(new ArrayList<>());
    }

    // sendResetEmail

    @Test
    void sendResetEmail_shouldSendMailWithNewPasswordInBody() {
        userUtils.sendResetEmail("jan@test.pl", "noweHaslo123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("jan@test.pl");
        assertThat(sent.getSubject()).isEqualTo(Messages.RESET_PASSWORD_EMAIL_SUBJECT);
        assertThat(sent.getText()).isEqualTo(Messages.RESET_PASSWORD_EMAIL_BODY + "noweHaslo123");
    }

    // createUser

    @Test
    void createUser_shouldEncodePasswordAndReturnDisabledUser() {
        when(passwordEncoder.encode("jawneHaslo")).thenReturn("$2a$10$zakodowane");

        User created = userUtils.createUser("Anna Nowak", "anna@test.pl", "jawneHaslo");

        assertThat(created.getName()).isEqualTo("Anna Nowak");
        assertThat(created.getEmail()).isEqualTo("anna@test.pl");
        assertThat(created.getPassword()).isEqualTo("$2a$10$zakodowane");
        assertThat(created.isEnabled()).isFalse();
    }

    @Test
    void createUser_shouldNeverStoreRawPassword() {
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$zakodowane");

        User created = userUtils.createUser("Anna Nowak", "anna@test.pl", "jawneHaslo");

        assertThat(created.getPassword()).isNotEqualTo("jawneHaslo");
        verify(passwordEncoder).encode("jawneHaslo");
    }

    // addWelcomeNotification

    @Test
    void addWelcomeNotification_shouldAppendNotificationFromSystem() {
        userUtils.addWelcomeNotification(user);

        assertThat(user.getNotifications()).hasSize(1);
        assertThat(user.getNotifications().get(0).getDescription()).isEqualTo(Messages.WELCOME_MESSAGE);
        assertThat(user.getNotifications().get(0).getCreator()).isEqualTo("System");
        assertThat(user.getNotifications().get(0).getUser()).isEqualTo(user);
        assertThat(user.getNotifications().get(0).getDate()).isNotNull();
    }

    @Test
    void addWelcomeNotification_shouldKeepExistingNotifications() {
        userUtils.addWelcomeNotification(user);
        userUtils.addWelcomeNotification(user);

        assertThat(user.getNotifications()).hasSize(2);
    }

    // emailExists

    @Test
    void emailExists_shouldReturnTrue_whenUserFound() {
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(user);

        assertThat(userUtils.emailExists("jan@test.pl")).isTrue();
    }

    @Test
    void emailExists_shouldReturnFalse_whenUserNotFound() {
        when(userRepository.findByEmail("brak@test.pl")).thenReturn(null);

        assertThat(userUtils.emailExists("brak@test.pl")).isFalse();
    }

    // removeClientFromConversations

    @Test
    void removeClientFromConversations_shouldDetachAndDeleteOnlyMatchingConversations() {
        Conversation clientConversation = new Conversation();
        clientConversation.setId(10L);
        clientConversation.setClient(user);

        User otherClient = new User("Inny", "inny@test.pl", "haslo", true);
        otherClient.setId(2L);
        Conversation otherConversation = new Conversation();
        otherConversation.setId(11L);
        otherConversation.setClient(otherClient);

        when(conversationRepository.findAll()).thenReturn(List.of(clientConversation, otherConversation));

        userUtils.removeClientFromConversations(1L);

        assertThat(clientConversation.getClient()).isNull();
        verify(conversationRepository).delete(clientConversation);
        verify(conversationRepository, never()).delete(otherConversation);
        assertThat(otherConversation.getClient()).isEqualTo(otherClient);
    }

    @Test
    void removeClientFromConversations_shouldIgnoreConversationsWithoutClient() {
        Conversation noClient = new Conversation();
        noClient.setId(12L);
        noClient.setClient(null);

        when(conversationRepository.findAll()).thenReturn(List.of(noClient));

        userUtils.removeClientFromConversations(1L);

        verify(conversationRepository, never()).delete(any());
    }

    // removeUserFromConversations

    @Test
    void removeUserFromConversations_shouldRemoveUserFromParticipantsAndSave() {
        User otherParticipant = new User("Anna", "anna@test.pl", "haslo", true);
        otherParticipant.setId(2L);

        Conversation conversation = new Conversation();
        conversation.setId(20L);
        conversation.setParticipants(new ArrayList<>(List.of(user, otherParticipant)));

        when(conversationRepository.findByParticipants_Id(1L)).thenReturn(List.of(conversation));

        userUtils.removeUserFromConversations(user);

        assertThat(conversation.getParticipants()).containsExactly(otherParticipant);
        verify(conversationRepository).save(conversation);
    }

    @Test
    void removeUserFromConversations_shouldDoNothing_whenUserHasNoConversations() {
        when(conversationRepository.findByParticipants_Id(1L)).thenReturn(List.of());

        userUtils.removeUserFromConversations(user);

        verify(conversationRepository, never()).save(any());
    }
}
