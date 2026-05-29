package com.sportreserve.court.dto;

import com.sportreserve.court.CourtType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CourtResponse(
    UUID id,
    String name,
    CourtType type,
    String description,
    Integer durationMinutes,
    BigDecimal price,
    String imageUrl,
    Boolean isActive,
    List<String> amenities
) {}
