package pl.lukbol.dyplom.DTOs.chat;

public record MessageDTO(
        Long id,
        Long senderId,
        String senderName,
        String content,
        String messageDate,
        Long conversationId
) {}
