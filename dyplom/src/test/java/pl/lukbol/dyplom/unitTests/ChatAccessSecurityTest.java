package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.ChatService;
import pl.lukbol.dyplom.services.MessageService;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAccessSecurityTest {
    @Mock UserRepository userRepository;
    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock MessageService messageService;
    @InjectMocks ChatService chatService;

    private User user(long id, String email) {
        User u = new User(); u.setId(id); u.setEmail(email); return u;
    }
    @Test void unrelatedUserCannotReadMessages() {
        User owner = user(1L, "owner@example.com");
        User intruder = user(2L, "intruder@example.com");
        Conversation c = new Conversation(); c.setId(10L);
        c.setClient(owner); c.setParticipants(List.of(owner));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(c));
        when(userRepository.findByEmail("intruder@example.com")).thenReturn(intruder);
        assertThatThrownBy(() -> chatService.getMessagesForConversation(10L, "intruder@example.com"))
                .isInstanceOf(AccessDeniedException.class);
        verify(messageRepository, never()).findByConversation(any());
    }
    @Test void clientCanReadMessages() {
        User owner = user(1L, "owner@example.com");
        Conversation c = new Conversation(); c.setId(10L); c.setClient(owner); c.setParticipants(List.of());
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(c));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(owner);
        when(messageRepository.findByConversation(c)).thenReturn(List.of());
        assertThat(chatService.getMessagesForConversation(10L, "owner@example.com")).isEmpty();
    }
    @Test void participantCanReadMessages() {
        User owner = user(1L, "owner@example.com");
        User participant = user(2L, "participant@example.com");
        Conversation c = new Conversation(); c.setId(10L); c.setClient(owner); c.setParticipants(List.of(participant));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(c));
        when(userRepository.findByEmail("participant@example.com")).thenReturn(participant);
        when(messageRepository.findByConversation(c)).thenReturn(List.of());
        assertThat(chatService.getMessagesForConversation(10L, "participant@example.com")).isEmpty();
    }
}
