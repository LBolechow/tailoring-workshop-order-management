package pl.lukbol.dyplom.DTOs.order;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.lukbol.dyplom.common.Messages;

import java.util.List;

public record AddOrderDTO(
        @NotBlank(message = Messages.ORDER_DESCRIPTION_REQUIRED)
        @Size(max = 255, message = Messages.VALIDATION_ORDER_DESCRIPTION_TOO_LONG)
        String description,

        @NotBlank(message = Messages.ORDER_CLIENT_REQUIRED)
        @Size(max = 255, message = Messages.VALIDATION_ORDER_CLIENT_TOO_LONG)
        String clientName,

        @NotBlank(message = Messages.ORDER_EMAIL_REQUIRED)
        @Email(message = Messages.ORDER_EMAIL_INVALID)
        @Size(max = 254, message = Messages.VALIDATION_EMAIL_TOO_LONG)
        String email,

        @NotBlank(message = Messages.ORDER_PHONE_REQUIRED)
        @Size(max = 32, message = Messages.VALIDATION_ORDER_PHONE_TOO_LONG)
        String phoneNumber,

        @DecimalMin(value = "0.5", message = Messages.ORDER_DURATION_MIN)
        @DecimalMax(value = "8.0", message = Messages.ORDER_DURATION_MAX)
        double hours,

        // Format and time zone are validated by DateUtils and BookingPolicy, not duplicated here.
        @NotBlank(message = Messages.ORDER_START_REQUIRED)
        @Size(max = 40, message = Messages.VALIDATION_ORDER_DATE_TOO_LONG)
        String startDate,

        @NotBlank(message = Messages.ORDER_END_REQUIRED)
        @Size(max = 40, message = Messages.VALIDATION_ORDER_DATE_TOO_LONG)
        String endDate,

        @NotBlank(message = Messages.ORDER_EMPLOYEE_REQUIRED)
        @Size(max = 255, message = Messages.VALIDATION_ORDER_EMPLOYEE_TOO_LONG)
        String selectedUser,

        @Min(value = 0, message = Messages.ORDER_PRICE_NONNEGATIVE)
        int price,

        @NotBlank(message = Messages.ORDER_STATUS_REQUIRED)
        @Pattern(regexp = Messages.ORDER_STATUS_IN_PROGRESS + "|" + Messages.ORDER_STATUS_COMPLETED + "|" + Messages.ORDER_STATUS_CANCELLED,
                message = Messages.VALIDATION_ORDER_STATUS_INVALID)
        String status,

        // An order may legitimately require zero materials.
        @NotNull(message = Messages.ORDER_ITEMS_REQUIRED)
        @Size(max = 100, message = Messages.VALIDATION_ORDER_ITEMS_TOO_MANY)
        List<@NotBlank(message = Messages.VALIDATION_ORDER_ITEM_REQUIRED)
             @Size(max = 255, message = Messages.VALIDATION_ORDER_ITEM_TOO_LONG) String> items
) {}
