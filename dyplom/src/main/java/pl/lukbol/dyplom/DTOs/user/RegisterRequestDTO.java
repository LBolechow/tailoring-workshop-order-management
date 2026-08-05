package pl.lukbol.dyplom.DTOs.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank(message = "Imię i nazwisko jest wymagane.")
        String name,

        @NotBlank(message = "Adres email jest wymagany.")
        @Email(message = "Nieprawidłowy format adresu email.")
        String email,

        @NotBlank(message = "Hasło jest wymagane.")
        @Size(min = 8, message = "Hasło musi mieć co najmniej 8 znaków.")
        String password
) {}