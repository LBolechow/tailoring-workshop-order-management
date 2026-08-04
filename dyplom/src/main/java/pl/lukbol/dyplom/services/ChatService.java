package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.DTOs.chat.SendMessageDTO;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Message;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.DTOs.conversation.ConversationResponse;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository userRepository;


    private final MessageRepository messageRepository;

    private final ConversationRepository conversationRepository;

    private final MessageService messageService;


    @Transactional
    public Message sendMessageToClient(Conversation conversation, Message message) {

        SendMessageDTO dto = new SendMessageDTO(message.getSender(), conversation, message.getContent(), message.getMessageDate());
        messageService.sendMessage(dto);
        return message;
    }

    @Transactional
    public Message sendMessageToEmployees(Message message) {
        String clientEmail = message.getSender().getEmail();

        User client = userRepository.findByEmail(clientEmail);

        List<Conversation> conversations = conversationRepository.findConversationByClient_Id(client.getId());

        if (conversations == null || conversations.isEmpty()) {
            conversations = new ArrayList<>();


            Conversation conversation = new Conversation();
            conversation.setClient(client);
            conversation.setName(client.getName());
            conversation.setOdczyt(false);
            conversation = conversationRepository.save(conversation);
            conversations.add(conversation);
            client.setConversations(conversations);
            userRepository.save(client);

        }

        for (Conversation conversation : conversations) {
            SendMessageDTO dto = new SendMessageDTO( message.getSender(), conversation, message.getContent(), message.getMessageDate());
            messageService.sendMessage(dto);
            conversation.getSeenByUserIds().clear();
            conversation.setOdczyt(false);
            conversationRepository.save(conversation);
        }
        return message;
    }

    public ResponseEntity<List<Message>> getClientConversation(Authentication authentication) {
        User user = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));

        List<Conversation> conversations = conversationRepository.findConversationByClient_Id(user.getId());
        if (!conversations.isEmpty()) {
            List<Message> messages = messageRepository.findByConversation(conversations.get(0));
            if (!messages.isEmpty()) {
                return ResponseEntity.ok(messages);
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<List<Message>> getAllEmployeeConversationMessages(Authentication authentication) {
        User user = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        List<Conversation> conversations = conversationRepository.findByParticipants_Id(user.getId());

        List<Message> allMessages = new ArrayList<>();
        conversations.forEach(conversation -> {
            List<Message> messages = messageRepository.findByConversation(conversation);
            allMessages.addAll(messages);
        });

        if (!allMessages.isEmpty()) {
            return ResponseEntity.ok(allMessages);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    public ResponseEntity<ConversationResponse> createConversation(
            Authentication authentication,
            String name,
            String participantIds
    ) {
        User user = userRepository.findByEmail(
                AuthenticationUtils.checkmail(authentication.getPrincipal())
        );

        try {
            List<Long> participantsIdsList = Arrays.stream(participantIds.split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            List<User> participants = userRepository.findAllById(participantsIdsList);
            participants.add(user);

            Conversation newConversation = new Conversation(name, participants, new ArrayList<>(), false);
            conversationRepository.save(newConversation);

            return ResponseEntity.ok(
                    new ConversationResponse(true, Messages.CONVERSATION_CREATED)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ConversationResponse(false,
                            Messages.CONVERSATION_CREATE_ERROR + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<String> markConversationAsRead(Authentication authentication, Long conversationId) {
        User user = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);

        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            Set<String> users = conversation.getSeenByUserIds();
            users.add(user.getId().toString());
            conversation.setSeenByUserIds(users);
            conversationRepository.save(conversation);

            return ResponseEntity.ok(Messages.CONVERSATION_MARKED_AS_READ);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    public ResponseEntity<String> clearSeenByUserIds(Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);

        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            conversation.getSeenByUserIds().clear();
            conversationRepository.save(conversation);

            return ResponseEntity.ok(Messages.CONVERSATION_SEEN_CLEARED);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<Boolean> checkIfConversationRead(Authentication authentication, Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        User user = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));

        if (conversationOptional.isPresent() && user != null) {
            Conversation conversation = conversationOptional.get();
            Set<String> seenByUserIds = conversation.getSeenByUserIds();
            boolean isRead = seenByUserIds.contains(user.getId().toString());
            return ResponseEntity.ok(isRead);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<List<User>> getConversationParticipants(Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            List<User> participants = new ArrayList<>(conversation.getParticipants());
            if (participants.isEmpty()) {
                participants.add(conversation.getClient());
            }
            return ResponseEntity.ok(participants);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<List<User>> getParticipantsBySeen(Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            List<User> participants = conversation.getParticipants();
            Set<String> seenList = conversation.getSeenByUserIds();
            List<User> seenParticipants = participants.stream()
                    .filter(user -> seenList.contains(user.getId().toString()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(seenParticipants);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    public Message getLatestMessageForConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApplicationException.ConversationNotFoundException(Messages.CONVERSATION_NOT_FOUND));

        Message latestMessage = messageRepository.findTopByConversationOrderByMessageDateDesc(conversation);

        if (latestMessage == null) {
            throw new ApplicationException.LastMessageNotFoundException(Messages.LAST_MESSAGE_NOT_FOUND);
        }

        return latestMessage;
    }

    @Transactional
    public ResponseEntity<String> hideConversation(Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);

        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            boolean currentlyRead = conversation.isOdczyt();
            conversation.setOdczyt(!currentlyRead);
            conversationRepository.save(conversation);

            if (currentlyRead) {
                return ResponseEntity.ok(Messages.CONVERSATION_RESTORED);
            } else {
                return ResponseEntity.ok(Messages.CONVERSATION_HIDDEN);
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
