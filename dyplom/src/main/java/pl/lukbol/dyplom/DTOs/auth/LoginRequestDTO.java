package pl.lukbol.dyplom.DTOs.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.lukbol.dyplom.common.Messages;

public record LoginRequestDTO(
        @NotBlank(message = Messages.VALIDATION_EMAIL_REQUIRED)
        @Email(message = Messages.VALIDATION_EMAIL_INVALID)
        @Size(max = 254, message = Messages.VALIDATION_EMAIL_TOO_LONG)
        String email,

        @NotBlank(message = Messages.VALIDATION_PASSWORD_REQUIRED)
        String password
) {
    @Override
    public String toString() {
        return "LoginRequestDTO[redacted]";
    }
}
