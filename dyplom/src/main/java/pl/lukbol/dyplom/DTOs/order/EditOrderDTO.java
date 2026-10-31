package pl.lukbol.dyplom.DTOs.order;

import pl.lukbol.dyplom.common.Messages;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EditOrderDTO(
        @NotBlank(message = Messages.ORDER_DESCRIPTION_REQUIRED)
        String description,

        @NotBlank(message = Messages.ORDER_CLIENT_REQUIRED)
        String clientName,

        @NotBlank(message = Messages.ORDER_EMAIL_REQUIRED)
        @Email(message = Messages.ORDER_EMAIL_INVALID)
        String email,

        @NotBlank(message = Messages.ORDER_PHONE_REQUIRED)
        String phoneNumber,

        @DecimalMin(value = "0.5", message = Messages.ORDER_DURATION_MIN)
        @DecimalMax(value = "8.0", message = Messages.ORDER_DURATION_MAX)
        double hours,

        @NotBlank(message = Messages.ORDER_START_REQUIRED)
        String startDate,

        @NotBlank(message = Messages.ORDER_END_REQUIRED)
        String endDate,

        @NotBlank(message = Messages.ORDER_EMPLOYEE_REQUIRED)
        String selectedUser,

        @Min(value = 0, message = Messages.ORDER_PRICE_NONNEGATIVE)
        int price,

        @NotBlank(message = Messages.ORDER_STATUS_REQUIRED)
        String status,

        @NotNull(message = Messages.ORDER_ITEMS_REQUIRED)
        List<String> items
) {}