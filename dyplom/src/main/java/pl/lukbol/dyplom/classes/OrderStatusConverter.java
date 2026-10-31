package pl.lukbol.dyplom.classes;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pl.lukbol.dyplom.common.OrderStatus;

/** Keeps the original database values: 'W trakcie', 'Zakończone', 'Anulowane'. */
@Converter
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {
    @Override public String convertToDatabaseColumn(OrderStatus status) {
        return status == null ? null : status.label();
    }
    @Override public OrderStatus convertToEntityAttribute(String value) {
        return value == null ? null : OrderStatus.fromLabel(value);
    }
}
