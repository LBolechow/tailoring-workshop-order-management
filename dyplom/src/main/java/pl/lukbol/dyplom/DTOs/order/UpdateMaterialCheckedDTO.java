package pl.lukbol.dyplom.DTOs.order;

import jakarta.validation.constraints.NotNull;
import pl.lukbol.dyplom.common.Messages;

/** A missing checked value must not silently reset the material to false. */
public record UpdateMaterialCheckedDTO(
        @NotNull(message = Messages.VALIDATION_MATERIAL_CHECKED_REQUIRED)
        Boolean checked
) {}
