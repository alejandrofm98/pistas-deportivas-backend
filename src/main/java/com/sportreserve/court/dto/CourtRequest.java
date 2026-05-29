package com.sportreserve.court.dto;

import com.sportreserve.court.CourtType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CourtRequest(
    @NotBlank String name,
    @NotNull CourtType type,
    String description,
    @NotNull @Min(30) @Max(120) Integer durationMinutes,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    String imageUrl,
    List<String> amenities
) {}
