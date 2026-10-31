package pl.lukbol.dyplom.DTOs.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.lukbol.dyplom.common.Messages;

/** The current profile endpoint replaces both the display name and password. */
public record UpdateProfileRequestDTO(
        @NotBlank(message = Messages.VALIDATION_NAME_REQUIRED)
        @Size(max = 255, message = Messages.VALIDATION_NAME_TOO_LONG)
        String username,

        @NotBlank(message = Messages.VALIDATION_PASSWORD_REQUIRED)
        @Size(min = 8, message = Messages.VALIDATION_PASSWORD_MIN_8)
        @Size(max = 128, message = Messages.VALIDATION_PASSWORD_TOO_LONG)
        String password,

        @NotBlank(message = Messages.VALIDATION_REPEAT_PASSWORD_REQUIRED)
        String repeatPassword
) {
    @Override
    public String toString() {
        return "UpdateProfileRequestDTO[redacted]";
    }
}
