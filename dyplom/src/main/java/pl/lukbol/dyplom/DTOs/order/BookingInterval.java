package pl.lukbol.dyplom.DTOs.order;

import java.util.Date;

public record BookingInterval(Long employeeId, Date startDate, Date endDate) {
}
