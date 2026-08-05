package pl.lukbol.dyplom.utilities;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Notification;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserUtils {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    private final ConversationRepository conversationRepository;

    private final PasswordEncoder passwordEncoder;


    public void sendResetEmail(String to, String newPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(Messages.RESET_PASSWORD_EMAIL_SUBJECT);
        message.setText(Messages.RESET_PASSWORD_EMAIL_BODY + newPassword);
        message.setTo(to);
        mailSender.send(message);
    }

    public User createUser(String name, String email, String password) {
        return new User(name, email, passwordEncoder.encode(password), false);
    }

    public void addWelcomeNotification(User user) {
        List<Notification> notifications = user.getNotifications();
        notifications.add(new Notification(Messages.WELCOME_MESSAGE, new Date(), user, "System"));
        user.setNotifications(notifications);
    }


    public void removeClientFromConversations(Long clientId) {
        List<Conversation> conversationsToUpdate = conversationRepository.findAll()
                .stream()
                .filter(conversation -> conversation.getClient() != null && clientId.equals(conversation.getClient().getId()))
                .toList();

        for (Conversation conversation : conversationsToUpdate) {
            conversation.setClient(null);
            conversationRepository.save(conversation);
            conversationRepository.delete(conversation);
        }
    }

    public void removeUserFromConversations(User user) {
        List<Conversation> userConversations = conversationRepository.findByParticipants_Id(user.getId());
        for (Conversation conversation : userConversations) {
            conversation.getParticipants().remove(user);
            conversationRepository.save(conversation);
        }
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email) != null;
    }
}
