package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.DTOs.notification.NotificationRequestDTO;
import pl.lukbol.dyplom.classes.Notification;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.NotificationRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.util.Date;
import java.util.List;

import static pl.lukbol.dyplom.utilities.AuthenticationUtils.checkmail;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;

    private final NotificationRepository notificationRepository;

    @Transactional
    public ApiResponseDTO removeAlerts(Authentication authentication){
        User user = userRepository.findByEmail(checkmail(authentication.getPrincipal()));
        if (user == null) {
           throw new ApplicationException.UserNotFoundException(Messages.USER_NOT_FOUND_BY_EMAIL);
        }
        user.getNotifications().clear();
        notificationRepository.deleteAllByUserId(user.getId());
        userRepository.save(user);
        return new ApiResponseDTO(Messages.ALERTS_REMOVED_MSG);
    }

    @Transactional
    public ApiResponseDTO createNotification(String userEmail, NotificationRequestDTO notificationRequestDTO) {
        User sender = userRepository.findByEmail(userEmail);
        List<User> participants = userRepository.findByIdIn(notificationRequestDTO.participantIds());

        if (participants.isEmpty()) {
            throw new ApplicationException.ParticipantsListIsEmptyException(Messages.PARTICIPANTS_LIST_IS_EMPTY);
        }

        Date currentDate = new Date();
        for (User participant : participants) {
            Notification notification = new Notification(notificationRequestDTO.message(), currentDate, participant, sender.getName());
            participant.getNotifications().add(notification);
            userRepository.save(participant);
        }

        return new ApiResponseDTO(Messages.NOTIFICATION_CREATED_SUCCESS_MSG);
    }
}
