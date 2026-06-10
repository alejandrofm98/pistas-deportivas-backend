package com.sportreserve.court.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
    LocalDate date,
    List<TimeSlot> slots
) {
    public record TimeSlot(Double time, boolean available) {}
}
