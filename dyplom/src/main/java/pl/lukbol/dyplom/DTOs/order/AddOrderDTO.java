package pl.lukbol.dyplom.DTOs.order;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddOrderDTO(
        @NotBlank(message = "Opis zlecenia jest wymagany.")
        String description,

        @NotBlank(message = "Nazwa klienta jest wymagana.")
        String clientName,

        @NotBlank(message = "Adres email klienta jest wymagany.")
        @Email(message = "Nieprawidłowy format adresu email.")
        String email,

        @NotBlank(message = "Numer telefonu jest wymagany.")
        String phoneNumber,

        @DecimalMin(value = "0.5", message = "Czas realizacji musi wynosić co najmniej 0.5 godziny.")
        @DecimalMax(value = "8.0", message = "Czas realizacji nie może przekraczać 8 godzin.")
        double hours,

        @NotBlank(message = "Data rozpoczęcia jest wymagana.")
        String startDate,

        @NotBlank(message = "Data zakończenia jest wymagana.")
        String endDate,

        @NotBlank(message = "Pracownik jest wymagany.")
        String selectedUser,

        @Min(value = 0, message = "Cena nie może być ujemna.")
        int price,

        @NotBlank(message = "Status zlecenia jest wymagany.")
        String status,

        @NotNull(message = "Lista materiałów jest wymagana.")
        List<String> items
) {}