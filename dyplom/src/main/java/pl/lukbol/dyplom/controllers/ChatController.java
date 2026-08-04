package pl.lukbol.dyplom.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.DTOs.chat.MessageDTO;
import pl.lukbol.dyplom.DTOs.conversation.ConversationDTO;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.DTOs.user.UserDTO;
import pl.lukbol.dyplom.classes.Message;
import pl.lukbol.dyplom.services.ChatService;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/sendToConversation/{conversationId}")
    @SendTo("/topic/employees")
    public MessageDTO sendMessageToConversation(@DestinationVariable Long conversationId, Message message) {
        return chatService.sendMessageToConversation(conversationId, message);
    }

    @MessageMapping("/sendToEmployees")
    @SendTo("/topic/employees")
    public MessageDTO sendMessageToEmployees(Message message) {
        return chatService.sendMessageToEmployees(message);
    }

    @GetMapping("/api/conversation")
    public ResponseEntity<List<MessageDTO>> getClientConversation(Authentication authentication) {
        return ResponseEntity.ok(chatService.getClientConversation(currentEmail(authentication)));
    }

    @GetMapping("/api/employee/conversations")
    public ResponseEntity<List<MessageDTO>> getAllEmployeeConversationMessages(Authentication authentication) {
        return ResponseEntity.ok(chatService.getAllEmployeeConversationMessages(currentEmail(authentication)));
    }

    @GetMapping("/get_conversations")
    public ResponseEntity<List<ConversationDTO>> getAllConversations() {
        return ResponseEntity.ok(chatService.getAllConversations());
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getMessagesForConversation(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.getMessagesForConversation(conversationId));
    }

    @GetMapping("/{conversationId}/latest-message")
    public ResponseEntity<MessageDTO> getLatestMessageForConversation(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.getLatestMessageForConversation(conversationId));
    }

    @PostMapping("/api/createConversation")
    public ResponseEntity<ApiResponseDTO> createConversation(
            Authentication authentication,
            @RequestParam("name") String name,
            @RequestParam("participantIds") String participantIds
    ) {
        return ResponseEntity.ok(
                chatService.createConversation(currentEmail(authentication), name, participantIds)
        );
    }

    @PostMapping("/markConversationAsRead/{conversationId}")
    public ResponseEntity<ApiResponseDTO> markConversationAsRead(Authentication authentication,
                                                                 @PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.markConversationAsRead(currentEmail(authentication), conversationId));
    }

    @PutMapping("/clearSeenByUserIds/{conversationId}")
    public ResponseEntity<ApiResponseDTO> clearSeenByUserIds(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.clearSeenByUserIds(conversationId));
    }

    @GetMapping("/checkIfConversationRead/{conversationId}")
    public ResponseEntity<Boolean> checkIfConversationRead(Authentication authentication,
                                                           @PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.checkIfConversationRead(currentEmail(authentication), conversationId));
    }

    @GetMapping("/getConversationParticipants/{conversationId}")
    public ResponseEntity<List<UserDTO>> getConversationParticipants(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.getConversationParticipants(conversationId));
    }

    @GetMapping("/checkSeen/{conversationId}")
    public ResponseEntity<List<UserDTO>> getParticipantsBySeen(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.getParticipantsBySeen(conversationId));
    }

    @PostMapping("/hide/{conversationId}")
    public ResponseEntity<ApiResponseDTO> hideConversation(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.hideConversation(conversationId));
    }

    private String currentEmail(Authentication authentication) {
        return AuthenticationUtils.checkmail(authentication.getPrincipal());
    }
}