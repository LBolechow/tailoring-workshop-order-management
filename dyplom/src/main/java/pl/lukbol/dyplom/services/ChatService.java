package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.DTOs.chat.MessageDTO;
import pl.lukbol.dyplom.DTOs.chat.SendMessageDTO;
import pl.lukbol.dyplom.DTOs.conversation.ConversationDTO;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.DTOs.user.UserDTO;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Message;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageService messageService;

    @Transactional
    public MessageDTO sendMessageToConversation(Long conversationId, Message message) {
        Conversation conversation = findConversationOrThrow(conversationId);

        Message saved = messageService.sendMessage(new SendMessageDTO(
                message.getSender(),
                conversation,
                message.getContent(),
                message.getMessageDate()
        ));

        return toMessageDTO(saved);
    }

    @Transactional
    public MessageDTO sendMessageToEmployees(Message message) {
        User client = findUserByEmailOrThrow(message.getSender().getEmail());

        List<Conversation> conversations = conversationRepository.findConversationByClient_Id(client.getId());
        if (conversations == null || conversations.isEmpty()) {
            conversations = List.of(createClientConversation(client));
        }

        Message lastSaved = null;
        for (Conversation conversation : conversations) {
            lastSaved = messageService.sendMessage(new SendMessageDTO(
                    client,
                    conversation,
                    message.getContent(),
                    message.getMessageDate()
            ));
            conversation.getSeenByUserIds().clear();
            conversation.setHidden(false);
            conversationRepository.save(conversation);
        }

        return toMessageDTO(lastSaved);
    }

    public List<MessageDTO> getClientConversation(String userEmail) {
        User user = findUserByEmailOrThrow(userEmail);

        List<Conversation> conversations = conversationRepository.findConversationByClient_Id(user.getId());
        if (conversations.isEmpty()) {
            throw new ApplicationException.ConversationNotFoundException(Messages.CONVERSATION_NOT_FOUND);
        }

        return toMessageDTOs(messageRepository.findByConversation(conversations.get(0)));
    }

    public List<MessageDTO> getAllEmployeeConversationMessages(String userEmail) {
        User user = findUserByEmailOrThrow(userEmail);

        List<Message> allMessages = conversationRepository.findByParticipants_Id(user.getId()).stream()
                .flatMap(conversation -> messageRepository.findByConversation(conversation).stream())
                .toList();

        return toMessageDTOs(allMessages);
    }

    public List<MessageDTO> getMessagesForConversation(Long conversationId) {
        Conversation conversation = findConversationOrThrow(conversationId);
        return toMessageDTOs(messageRepository.findByConversation(conversation));
    }

    public List<ConversationDTO> getAllConversations() {
        return conversationRepository.findAll().stream()
                .map(this::toConversationDTO)
                .toList();
    }

    public MessageDTO getLatestMessageForConversation(Long conversationId) {
        Conversation conversation = findConversationOrThrow(conversationId);

        Message latestMessage = messageRepository.findTopByConversationOrderByMessageDateDesc(conversation);
        if (latestMessage == null) {
            throw new ApplicationException.LastMessageNotFoundException(Messages.LAST_MESSAGE_NOT_FOUND);
        }

        return toMessageDTO(latestMessage);
    }

    @Transactional
    public ApiResponseDTO createConversation(String userEmail, String name, String participantIds) {
        User creator = findUserByEmailOrThrow(userEmail);

        List<Long> ids = parseParticipantIds(participantIds);
        List<User> participants = new ArrayList<>(userRepository.findAllById(ids));
        if (participants.isEmpty()) {
            throw new ApplicationException.ParticipantsListIsEmptyException(Messages.PARTICIPANTS_LIST_IS_EMPTY);
        }
        if (participants.stream().noneMatch(p -> p.getId().equals(creator.getId()))) {
            participants.add(creator);
        }

        conversationRepository.save(new Conversation(name, participants, new ArrayList<>(), false));

        return new ApiResponseDTO(Messages.CONVERSATION_CREATED);
    }

    @Transactional
    public ApiResponseDTO markConversationAsRead(String userEmail, Long conversationId) {
        User user = findUserByEmailOrThrow(userEmail);
        Conversation conversation = findConversationOrThrow(conversationId);

        Set<String> seenByUserIds = conversation.getSeenByUserIds();
        seenByUserIds.add(user.getId().toString());
        conversation.setSeenByUserIds(seenByUserIds);
        conversationRepository.save(conversation);

        return new ApiResponseDTO(Messages.CONVERSATION_MARKED_AS_READ);
    }

    @Transactional
    public ApiResponseDTO clearSeenByUserIds(Long conversationId) {
        Conversation conversation = findConversationOrThrow(conversationId);

        conversation.getSeenByUserIds().clear();
        conversationRepository.save(conversation);

        return new ApiResponseDTO(Messages.CONVERSATION_SEEN_CLEARED);
    }

    public boolean checkIfConversationRead(String userEmail, Long conversationId) {
        User user = findUserByEmailOrThrow(userEmail);
        Conversation conversation = findConversationOrThrow(conversationId);

        return conversation.getSeenByUserIds().contains(user.getId().toString());
    }

    public List<UserDTO> getConversationParticipants(Long conversationId) {
        Conversation conversation = findConversationOrThrow(conversationId);

        List<User> participants = new ArrayList<>(conversation.getParticipants());
        if (participants.isEmpty() && conversation.getClient() != null) {
            participants.add(conversation.getClient());
        }

        return toUserDTOs(participants);
    }

    public List<UserDTO> getParticipantsBySeen(Long conversationId) {
        Conversation conversation = findConversationOrThrow(conversationId);

        Set<String> seenByUserIds = conversation.getSeenByUserIds();
        List<User> seenParticipants = conversation.getParticipants().stream()
                .filter(user -> seenByUserIds.contains(user.getId().toString()))
                .toList();

        return toUserDTOs(seenParticipants);
    }

    @Transactional
    public ApiResponseDTO hideConversation(Long conversationId) {
        Conversation conversation = findConversationOrThrow(conversationId);

        boolean currentlyHidden = conversation.isHidden();
        conversation.setHidden(!currentlyHidden);
        conversationRepository.save(conversation);

        return new ApiResponseDTO(
                currentlyHidden ? Messages.CONVERSATION_RESTORED : Messages.CONVERSATION_HIDDEN
        );
    }

    // --- helpers ---

    private Conversation createClientConversation(User client) {
        Conversation conversation = new Conversation();
        conversation.setClient(client);
        conversation.setName(client.getName());
        conversation.setHidden(false);
        return conversationRepository.save(conversation);
    }

    private List<Long> parseParticipantIds(String participantIds) {
        if (participantIds == null || participantIds.isBlank()) {
            throw new ApplicationException.ParticipantsListIsEmptyException(Messages.PARTICIPANTS_LIST_IS_EMPTY);
        }

        try {
            return Arrays.stream(participantIds.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new ApplicationException.ParticipantsListIsEmptyException(Messages.PARTICIPANTS_LIST_IS_EMPTY);
        }
    }

    private Conversation findConversationOrThrow(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApplicationException.ConversationNotFoundException(Messages.CONVERSATION_NOT_FOUND));
    }

    private User findUserByEmailOrThrow(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ApplicationException.UserNotFoundException(Messages.USER_NOT_FOUND_BY_EMAIL);
        }
        return user;
    }

    private MessageDTO toMessageDTO(Message message) {
        if (message == null) {
            return null;
        }

        User sender = message.getSender();
        Conversation conversation = message.getConversation();

        return new MessageDTO(
                message.getId(),
                sender != null ? sender.getId() : null,
                sender != null ? sender.getName() : null,
                message.getContent(),
                message.getMessageDate() != null
                        ? new SimpleDateFormat(DATE_PATTERN).format(message.getMessageDate())
                        : null,
                conversation != null ? conversation.getId() : null
        );
    }

    private List<MessageDTO> toMessageDTOs(List<Message> messages) {
        return messages.stream()
                .map(this::toMessageDTO)
                .toList();
    }

    private UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isEnabled(),
                user.getRole() != null ? user.getRole().getName() : null
        );
    }

    private List<UserDTO> toUserDTOs(List<User> users) {
        return users.stream()
                .filter(Objects::nonNull)
                .map(this::toUserDTO)
                .toList();
    }

    private ConversationDTO toConversationDTO(Conversation conversation) {
        return new ConversationDTO(
                conversation.getId(),
                conversation.getName(),
                conversation.isHidden(),
                conversation.getParticipants() != null
                        ? toUserDTOs(conversation.getParticipants())
                        : List.of(),
                toUserDTO(conversation.getClient())
        );
    }
}