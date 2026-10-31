package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.OrderStatusConverter;
import pl.lukbol.dyplom.common.OrderStatus;
import pl.lukbol.dyplom.exceptions.ApplicationException;

import static org.assertj.core.api.Assertions.*;

class OrderStatusTest {
    @Test void preservesExistingDatabaseValuesAndDtoLabels() {
        Order order = new Order();
        order.setStatus("W trakcie");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(order.getStatus()).isEqualTo("W trakcie");
        OrderStatusConverter converter = new OrderStatusConverter();
        assertThat(converter.convertToDatabaseColumn(order.getOrderStatus())).isEqualTo("W trakcie");
        assertThat(converter.convertToEntityAttribute("Zakończone")).isEqualTo(OrderStatus.COMPLETED);
    }
    @Test void unknownStatusIsRejected() {
        assertThatThrownBy(() -> new Order().setStatus("something invalid"))
                .isInstanceOf(ApplicationException.InvalidOrderStatusException.class);
    }
}
