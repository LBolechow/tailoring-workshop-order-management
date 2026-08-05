package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.lukbol.dyplom.DTOs.chat.MessageDTO;
import pl.lukbol.dyplom.DTOs.chat.SendMessageDTO;
import pl.lukbol.dyplom.DTOs.conversation.ConversationDTO;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.DTOs.user.UserDTO;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Message;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.ChatService;
import pl.lukbol.dyplom.services.MessageService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ChatService chatService;

    private User client;
    private User employee;
    private Conversation conversation;
    private Message message;

    @BeforeEach
    void setUp() {
        Role roleClient = new Role("ROLE_CLIENT");
        Role roleEmployee = new Role("ROLE_EMPLOYEE");

        client = new User("Jan Klient", "jan@test.pl", "haslo", true);
        client.setId(1L);
        client.setRole(roleClient);

        employee = new User("Anna Nowak", "anna@test.pl", "haslo", true);
        employee.setId(2L);
        employee.setRole(roleEmployee);

        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setName("Rozmowa testowa");
        conversation.setHidden(false);
        conversation.setSeenByUserIds(new HashSet<>());
        conversation.setParticipants(new ArrayList<>(List.of(employee)));
        conversation.setClient(client);

        message = new Message(client, "Treść wiadomości", conversation, new Date());
        message.setId(100L);
    }

    // sendMessageToConversation

    @Test
    void sendMessageToConversation_shouldSaveMessageAndReturnDTO() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageService.sendMessage(any(SendMessageDTO.class))).thenReturn(message);

        MessageDTO result = chatService.sendMessageToConversation(10L, message);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.content()).isEqualTo("Treść wiadomości");
        assertThat(result.senderId()).isEqualTo(1L);
        assertThat(result.senderName()).isEqualTo("Jan Klient");
        assertThat(result.conversationId()).isEqualTo(10L);
    }

    @Test
    void sendMessageToConversation_shouldThrowConversationNotFound_whenConversationMissing() {
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessageToConversation(99L, message))
                .isInstanceOf(ApplicationException.ConversationNotFoundException.class)
                .hasMessage(Messages.CONVERSATION_NOT_FOUND);

        verify(messageService, never()).sendMessage(any());
    }

    // sendMessageToEmployees

    @Test
    void sendMessageToEmployees_shouldResetSeenStatusAndUnhideConversation() {
        conversation.setHidden(true);
        conversation.getSeenByUserIds().add("2");

        when(userRepository.findByEmail("jan@test.pl")).thenReturn(client);
        when(conversationRepository.findConversationByClient_Id(1L)).thenReturn(List.of(conversation));
        when(messageService.sendMessage(any(SendMessageDTO.class))).thenReturn(message);

        chatService.sendMessageToEmployees(message);

        assertThat(conversation.getSeenByUserIds()).isEmpty();
        assertThat(conversation.isHidden()).isFalse();
        verify(conversationRepository).save(conversation);
    }

    @Test
    void sendMessageToEmployees_shouldCreateConversation_whenClientHasNone() {
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(client);
        when(conversationRepository.findConversationByClient_Id(1L)).thenReturn(List.of());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation created = inv.getArgument(0);
            created.setId(20L);
            return created;
        });
        when(messageService.sendMessage(any(SendMessageDTO.class))).thenReturn(message);

        chatService.sendMessageToEmployees(message);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getClient()).isEqualTo(client);
    }

    @Test
    void sendMessageToEmployees_shouldThrowUserNotFound_whenSenderUnknown() {
        User unknown = new User("Nieznany", "brak@test.pl", "haslo", true);
        Message fromUnknown = new Message(unknown, "test", conversation, new Date());
        when(userRepository.findByEmail("brak@test.pl")).thenReturn(null);

        assertThatThrownBy(() -> chatService.sendMessageToEmployees(fromUnknown))
                .isInstanceOf(ApplicationException.UserNotFoundException.class)
                .hasMessage(Messages.USER_NOT_FOUND_BY_EMAIL);
    }

    // getClientConversation

    @Test
    void getClientConversation_shouldReturnMessages() {
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(client);
        when(conversationRepository.findConversationByClient_Id(1L)).thenReturn(List.of(conversation));
        when(messageRepository.findByConversation(conversation)).thenReturn(List.of(message));

        List<MessageDTO> result = chatService.getClientConversation("jan@test.pl");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("Treść wiadomości");
    }

    @Test
    void getClientConversation_shouldThrowConversationNotFound_whenNoConversation() {
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(client);
        when(conversationRepository.findConversationByClient_Id(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> chatService.getClientConversation("jan@test.pl"))
                .isInstanceOf(ApplicationException.ConversationNotFoundException.class);
    }

    @Test
    void getClientConversation_shouldReturnEmptyList_whenConversationHasNoMessages() {
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(client);
        when(conversationRepository.findConversationByClient_Id(1L)).thenReturn(List.of(conversation));
        when(messageRepository.findByConversation(conversation)).thenReturn(List.of());

        assertThat(chatService.getClientConversation("jan@test.pl")).isEmpty();
    }

    // getAllEmployeeConversationMessages

    @Test
    void getAllEmployeeConversationMessages_shouldFlattenMessagesFromAllConversations() {
        Conversation second = new Conversation();
        second.setId(11L);
        Message otherMessage = new Message(employee, "Druga wiadomość", second, new Date());

        when(userRepository.findByEmail("anna@test.pl")).thenReturn(employee);
        when(conversationRepository.findByParticipants_Id(2L)).thenReturn(List.of(conversation, second));
        when(messageRepository.findByConversation(conversation)).thenReturn(List.of(message));
        when(messageRepository.findByConversation(second)).thenReturn(List.of(otherMessage));

        List<MessageDTO> result = chatService.getAllEmployeeConversationMessages("anna@test.pl");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MessageDTO::content)
                .containsExactly("Treść wiadomości", "Druga wiadomość");
    }

    // getLatestMessageForConversation

    @Test
    void getLatestMessageForConversation_shouldReturnLatest() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findTopByConversationOrderByMessageDateDesc(conversation)).thenReturn(message);

        MessageDTO result = chatService.getLatestMessageForConversation(10L);

        assertThat(result.content()).isEqualTo("Treść wiadomości");
    }

    @Test
    void getLatestMessageForConversation_shouldThrowLastMessageNotFound_whenNoMessages() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findTopByConversationOrderByMessageDateDesc(conversation)).thenReturn(null);

        assertThatThrownBy(() -> chatService.getLatestMessageForConversation(10L))
                .isInstanceOf(ApplicationException.LastMessageNotFoundException.class)
                .hasMessage(Messages.LAST_MESSAGE_NOT_FOUND);
    }

    // getAllConversations

    @Test
    void getAllConversations_shouldMapToDTOsWithoutExposingEntities() {
        when(conversationRepository.findAll()).thenReturn(List.of(conversation));

        List<ConversationDTO> result = chatService.getAllConversations();

        assertThat(result).hasSize(1);
        ConversationDTO dto = result.get(0);
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.name()).isEqualTo("Rozmowa testowa");
        assertThat(dto.hidden()).isFalse();
        assertThat(dto.participants()).hasSize(1);
        assertThat(dto.participants().get(0).name()).isEqualTo("Anna Nowak");
        assertThat(dto.client().email()).isEqualTo("jan@test.pl");
    }

    // createConversation

    @Test
    void createConversation_shouldSaveConversationWithParticipants() {
        when(userRepository.findByEmail("anna@test.pl")).thenReturn(employee);
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(client));

        ApiResponseDTO result = chatService.createConversation("anna@test.pl", "Nowa rozmowa", "1");

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());

        Conversation saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Nowa rozmowa");
        assertThat(saved.getParticipants()).contains(client, employee);
        assertThat(result.message()).isEqualTo(Messages.CONVERSATION_CREATED);
    }

    @Test
    void createConversation_shouldNotDuplicateCreator_whenAlreadyInParticipants() {
        when(userRepository.findByEmail("anna@test.pl")).thenReturn(employee);
        when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(employee));

        chatService.createConversation("anna@test.pl", "Rozmowa", "2");

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants()).hasSize(1);
    }

    @Test
    void createConversation_shouldThrowParticipantsEmpty_whenIdsBlank() {
        when(userRepository.findByEmail("anna@test.pl")).thenReturn(employee);

        assertThatThrownBy(() -> chatService.createConversation("anna@test.pl", "Rozmowa", "  "))
                .isInstanceOf(ApplicationException.ParticipantsListIsEmptyException.class);

        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createConversation_shouldThrowParticipantsEmpty_whenIdsMalformed() {
        when(userRepository.findByEmail("anna@test.pl")).thenReturn(employee);

        assertThatThrownBy(() -> chatService.createConversation("anna@test.pl", "Rozmowa", "1,abc,3"))
                .isInstanceOf(ApplicationException.ParticipantsListIsEmptyException.class);

        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createConversation_shouldThrowParticipantsEmpty_whenNoUsersFound() {
        when(userRepository.findByEmail("anna@test.pl")).thenReturn(employee);
        when(userRepository.findAllById(List.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() -> chatService.createConversation("anna@test.pl", "Rozmowa", "999"))
                .isInstanceOf(ApplicationException.ParticipantsListIsEmptyException.class);
    }

    // markConversationAsRead / clearSeenByUserIds / checkIfConversationRead

    @Test
    void markConversationAsRead_shouldAddUserIdToSeenSet() {
        when(userRepository.findByEmail("anna@test.pl")).thenReturn(employee);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        ApiResponseDTO result = chatService.markConversationAsRead("anna@test.pl", 10L);

        assertThat(conversation.getSeenByUserIds()).contains("2");
        verify(conversationRepository).save(conversation);
        assertThat(result.message()).isEqualTo(Messages.CONVERSATION_MARKED_AS_READ);
    }

    @Test
    void clearSeenByUserIds_shouldEmptySeenSet() {
        conversation.getSeenByUserIds().addAll(List.of("1", "2"));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        ApiResponseDTO result = chatService.clearSeenByUserIds(10L);

        assertThat(conversation.getSeenByUserIds()).isEmpty();
        assertThat(result.message()).isEqualTo(Messages.CONVERSATION_SEEN_CLEARED);
    }

    @Test
    void checkIfConversationRead_shouldReturnTrue_whenUserInSeenSet() {
        conversation.getSeenByUserIds().add("2");
        when(userRepository.findByEmail("anna@test.pl")).thenReturn(employee);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        assertThat(chatService.checkIfConversationRead("anna@test.pl", 10L)).isTrue();
    }

    @Test
    void checkIfConversationRead_shouldReturnFalse_whenUserNotInSeenSet() {
        when(userRepository.findByEmail("anna@test.pl")).thenReturn(employee);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        assertThat(chatService.checkIfConversationRead("anna@test.pl", 10L)).isFalse();
    }

    // getConversationParticipants

    @Test
    void getConversationParticipants_shouldReturnParticipantsAsDTOs() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        List<UserDTO> result = chatService.getConversationParticipants(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Anna Nowak");
        assertThat(result.get(0).role()).isEqualTo("ROLE_EMPLOYEE");
    }

    @Test
    void getConversationParticipants_shouldFallBackToClient_whenNoParticipants() {
        conversation.setParticipants(new ArrayList<>());
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        List<UserDTO> result = chatService.getConversationParticipants(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("jan@test.pl");
    }

    @Test
    void getConversationParticipants_shouldReturnEmptyList_whenNoParticipantsAndNoClient() {
        conversation.setParticipants(new ArrayList<>());
        conversation.setClient(null);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        assertThat(chatService.getConversationParticipants(10L)).isEmpty();
    }

    // getParticipantsBySeen

    @Test
    void getParticipantsBySeen_shouldReturnOnlyUsersWhoSawConversation() {
        User second = new User("Piotr Kowalski", "piotr@test.pl", "haslo", true);
        second.setId(3L);
        second.setRole(new Role("ROLE_EMPLOYEE"));
        conversation.setParticipants(new ArrayList<>(List.of(employee, second)));
        conversation.getSeenByUserIds().add("3");

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        List<UserDTO> result = chatService.getParticipantsBySeen(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Piotr Kowalski");
    }

    // hideConversation

    @Test
    void hideConversation_shouldHide_whenCurrentlyVisible() {
        conversation.setHidden(false);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        ApiResponseDTO result = chatService.hideConversation(10L);

        assertThat(conversation.isHidden()).isTrue();
        assertThat(result.message()).isEqualTo(Messages.CONVERSATION_HIDDEN);
    }

    @Test
    void hideConversation_shouldRestore_whenCurrentlyHidden() {
        conversation.setHidden(true);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        ApiResponseDTO result = chatService.hideConversation(10L);

        assertThat(conversation.isHidden()).isFalse();
        assertThat(result.message()).isEqualTo(Messages.CONVERSATION_RESTORED);
    }
}