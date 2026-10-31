package pl.lukbol.dyplom.DTOs.order;

import jakarta.validation.constraints.NotNull;
import pl.lukbol.dyplom.common.Messages;

public record UpdateMaterialCheckedDTO(
        @NotNull(message = Messages.VALIDATION_MATERIAL_CHECKED_REQUIRED)
        Boolean checked
) {}
