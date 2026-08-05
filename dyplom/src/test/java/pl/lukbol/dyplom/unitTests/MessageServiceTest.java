package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.lukbol.dyplom.DTOs.chat.SendMessageDTO;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Message;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.services.MessageService;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        sender = new User("Jan Kowalski", "jan@test.pl", "haslo", true);
        sender.setId(1L);

        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setName("Rozmowa testowa");
    }

    @Test
    void sendMessage_shouldPersistMessageWithAllFieldsFromDto() {
        Date sentAt = new Date(1_700_000_000_000L);
        SendMessageDTO dto = new SendMessageDTO(sender, conversation, "Dzien dobry", sentAt);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        messageService.sendMessage(dto);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());

        Message saved = captor.getValue();
        assertThat(saved.getSender()).isEqualTo(sender);
        assertThat(saved.getConversation()).isEqualTo(conversation);
        assertThat(saved.getContent()).isEqualTo("Dzien dobry");
        assertThat(saved.getMessageDate()).isEqualTo(sentAt);
    }

    @Test
    void sendMessage_shouldReturnMessageReturnedByRepository() {
        Message persisted = new Message(sender, "Tresc", conversation, new Date());
        persisted.setId(100L);

        SendMessageDTO dto = new SendMessageDTO(sender, conversation, "Tresc", new Date());
        when(messageRepository.save(any(Message.class))).thenReturn(persisted);

        Message result = messageService.sendMessage(dto);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getContent()).isEqualTo("Tresc");
    }
}
