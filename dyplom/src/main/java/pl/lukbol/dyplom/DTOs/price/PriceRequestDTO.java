package pl.lukbol.dyplom.DTOs.price;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.lukbol.dyplom.common.Messages;

/** Keep price as text to preserve the existing REST contract and catalog representation. */
public record PriceRequestDTO(
        @NotBlank(message = Messages.VALIDATION_PRICE_ITEM_REQUIRED)
        @Size(max = 255, message = Messages.VALIDATION_PRICE_ITEM_TOO_LONG)
        String item,

        @NotBlank(message = Messages.VALIDATION_PRICE_VALUE_REQUIRED)
        @Size(max = 255, message = Messages.VALIDATION_PRICE_VALUE_TOO_LONG)
        String price
) {}
