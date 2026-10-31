package pl.lukbol.dyplom.DTOs.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pl.lukbol.dyplom.common.Messages;

import java.util.List;

public record NotificationRequestDTO(
        @NotBlank(message = Messages.VALIDATION_NOTIFICATION_MESSAGE_REQUIRED)
        @Size(max = 255, message = Messages.VALIDATION_NOTIFICATION_MESSAGE_TOO_LONG)
        String message,

        @NotEmpty(message = Messages.VALIDATION_NOTIFICATION_RECIPIENTS_REQUIRED)
        @Size(max = 100, message = Messages.VALIDATION_NOTIFICATION_RECIPIENTS_TOO_MANY)
        List<@NotNull(message = Messages.VALIDATION_NOTIFICATION_RECIPIENT_INVALID)
             @Positive(message = Messages.VALIDATION_NOTIFICATION_RECIPIENT_INVALID) Long> participantIds
) {}
