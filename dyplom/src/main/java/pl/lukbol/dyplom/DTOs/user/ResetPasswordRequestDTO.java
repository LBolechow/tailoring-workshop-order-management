package pl.lukbol.dyplom.DTOs.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.lukbol.dyplom.common.Messages;

public record ResetPasswordRequestDTO(
        @NotBlank(message = Messages.VALIDATION_RESET_TOKEN_REQUIRED)
        @Size(max = 256, message = Messages.VALIDATION_RESET_TOKEN_TOO_LONG)
        String token,

        @NotBlank(message = Messages.VALIDATION_PASSWORD_REQUIRED)
        @Size(min = 12, message = Messages.VALIDATION_PASSWORD_MIN_12)
        @Size(max = 128, message = Messages.VALIDATION_PASSWORD_TOO_LONG)
        String newPassword
) {
    @Override
    public String toString() {
        return "ResetPasswordRequestDTO[redacted]";
    }
}
