package pl.lukbol.dyplom.DTOs.order;

import java.util.Date;

/** Lightweight read model for busy intervals: no Order entity or materials loaded. */
public record BookingInterval(Long employeeId, Date startDate, Date endDate) {
}
