package com.sportreserve.court.dto;

import com.sportreserve.court.CourtType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CourtRequest(
    @NotBlank String name,
    @NotNull CourtType type,
    String description,
    @NotNull @DecimalMin("0.01") BigDecimal pricePerHour,
    String imageUrl,
    List<String> amenities
) {}
