package pl.lukbol.dyplom.services;

import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.DTOs.order.OrderDTO;
import pl.lukbol.dyplom.DTOs.order.OrderDetailsDTO;
import pl.lukbol.dyplom.DTOs.material.MaterialDTO;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.utilities.DateUtils;
import java.util.List;

/** Converts persistent entities to DTOs; no repository access. */
@Component
public class OrderMapper {
    public OrderDTO toOrderDTO(Order order) {
        return new OrderDTO(
                order.getId(),
                order.getDescription(),
                order.getClientName(),
                order.getClientEmail(),
                order.getPhoneNumber(),
                order.getDuration(),
                DateUtils.formatOrderDateTime(order.getStartDate()),
                DateUtils.formatOrderDateTime(order.getEndDate()),
                order.getEmployeeName(),
                order.getPrice(),
                order.getStatus(),
                order.getMaterials().stream()
                        .map(m -> new MaterialDTO(m.getId(), m.getItem(), m.isChecked()))
                        .toList()
        );
    }

    public OrderDetailsDTO buildOrderDetailsDTO(Order order) {
        List<MaterialDTO> materials = order.getMaterials().stream()
                .map(m -> new MaterialDTO(m.getId(), m.getItem(), m.isChecked()))
                .toList();

        return new OrderDetailsDTO(
                order.getId(),
                order.getDescription(),
                order.getClientName(),
                order.getClientEmail(),
                order.getPhoneNumber(),
                order.getEmployeeName(),
                order.getStatus(),
                order.getPrice(),
                order.getDuration(),
                order.getIdCode(),
                materials
        );
    }


}
