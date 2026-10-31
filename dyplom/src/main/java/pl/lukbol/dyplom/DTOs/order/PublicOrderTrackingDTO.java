package pl.lukbol.dyplom.DTOs.order;

/** Safe response for a public tracking code: no name, email, telephone or token. */
public record PublicOrderTrackingDTO(String status, String startDate, String endDate) {}
