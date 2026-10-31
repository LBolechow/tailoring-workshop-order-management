package pl.lukbol.dyplom.DTOs.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.lukbol.dyplom.common.Messages;

public record ForgotPasswordRequestDTO(
        @NotBlank(message = Messages.VALIDATION_EMAIL_REQUIRED)
        @Email(message = Messages.VALIDATION_EMAIL_INVALID)
        @Size(max = 254, message = Messages.VALIDATION_EMAIL_TOO_LONG)
        String email
) {
    @Override
    public String toString() {
        return "ForgotPasswordRequestDTO[redacted]";
    }
}
