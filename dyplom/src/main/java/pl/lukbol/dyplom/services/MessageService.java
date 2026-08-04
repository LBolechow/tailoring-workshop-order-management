package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.DTOs.chat.SendMessageDTO;
import pl.lukbol.dyplom.classes.Message;
import pl.lukbol.dyplom.repositories.MessageRepository;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public Message sendMessage(SendMessageDTO sendMessageDTO) {
        Message message = new Message(
                sendMessageDTO.sender(),
                sendMessageDTO.content(),
                sendMessageDTO.conversation(),
                sendMessageDTO.messageDate()
        );
        return messageRepository.save(message);
    }
}