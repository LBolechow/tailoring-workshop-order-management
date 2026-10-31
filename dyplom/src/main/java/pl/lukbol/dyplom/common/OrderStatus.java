package pl.lukbol.dyplom.common;

import pl.lukbol.dyplom.exceptions.ApplicationException;

import java.util.Arrays;

/** Domain states; stored under their existing Polish labels to preserve rows. */
public enum OrderStatus {
    IN_PROGRESS(Messages.ORDER_STATUS_IN_PROGRESS),
    COMPLETED(Messages.ORDER_STATUS_COMPLETED),
    CANCELLED(Messages.ORDER_STATUS_CANCELLED);

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String label() { return label; }

    public static OrderStatus fromLabel(String value) {
        return Arrays.stream(values())
                .filter(status -> status.label.equals(value))
                .findFirst()
                .orElseThrow(() -> new ApplicationException.InvalidOrderStatusException(Messages.INVALID_ORDER_STATUS));
    }
}
