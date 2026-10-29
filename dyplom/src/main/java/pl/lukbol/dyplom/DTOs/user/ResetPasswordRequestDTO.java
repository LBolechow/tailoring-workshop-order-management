package pl.lukbol.dyplom.DTOs.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank String token,
        @NotBlank @Size(min = 12) String newPassword
) {}
