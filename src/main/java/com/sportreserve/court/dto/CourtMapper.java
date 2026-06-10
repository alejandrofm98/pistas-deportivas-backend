package com.sportreserve.court.dto;

import com.sportreserve.court.Court;
import org.springframework.stereotype.Component;

@Component
public class CourtMapper {

    public CourtResponse toResponse(Court court) {
        return new CourtResponse(
            court.getId(),
            court.getName(),
            court.getType(),
            court.getDescription(),
            court.getDurationMinutes(),
            court.getPrice(),
            court.getImageUrl(),
            court.getIsActive(),
            court.getAmenities()
        );
    }

    public Court toEntity(CourtRequest request) {
        Court court = new Court();
        court.setName(request.name());
        court.setType(request.type());
        court.setDescription(request.description());
        court.setDurationMinutes(request.durationMinutes());
        court.setPrice(request.price());
        court.setImageUrl(request.imageUrl());
        court.setAmenities(request.amenities());
        court.setIsActive(true);
        return court;
    }

    public void updateEntity(Court court, CourtRequest request) {
        court.setName(request.name());
        court.setType(request.type());
        court.setDescription(request.description());
        court.setDurationMinutes(request.durationMinutes());
        court.setPrice(request.price());
        court.setImageUrl(request.imageUrl());
        court.setAmenities(request.amenities());
    }
}
