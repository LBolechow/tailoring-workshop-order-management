package pl.lukbol.dyplom.DTOs.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.lukbol.dyplom.common.Messages;

public record UpdateUserRequest(
        @NotBlank(message = Messages.VALIDATION_NAME_REQUIRED)
        @Size(max = 255, message = Messages.VALIDATION_NAME_TOO_LONG)
        String name,

        @NotBlank(message = Messages.VALIDATION_EMAIL_REQUIRED)
        @Email(message = Messages.VALIDATION_EMAIL_INVALID)
        @Size(max = 254, message = Messages.VALIDATION_EMAIL_TOO_LONG)
        String email,

        @NotBlank(message = Messages.VALIDATION_ROLE_REQUIRED)
        @Pattern(regexp = "ROLE_(ADMIN|EMPLOYEE|CLIENT)", message = Messages.VALIDATION_ROLE_INVALID)
        String role
) {}
