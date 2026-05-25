package com.sportreserve.court.dto;

import com.sportreserve.court.Court;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class CourtMapper {

    public CourtResponse toResponse(Court court) {
        return new CourtResponse(
            court.getId(),
            court.getName(),
            court.getType(),
            court.getDescription(),
            court.getPricePerHour(),
            court.getImageUrl(),
            court.getIsActive(),
            court.getMaxPlayers(),
            court.getAmenities()
        );
    }

    public Court toEntity(CourtRequest request) {
        Court court = new Court();
        court.setName(request.name());
        court.setType(request.type());
        court.setDescription(request.description());
        court.setPricePerHour(request.pricePerHour());
        court.setImageUrl(request.imageUrl());
        court.setMaxPlayers(request.maxPlayers());
        court.setAmenities(request.amenities());
        court.setIsActive(true);
        return court;
    }

    public void updateEntity(Court court, CourtRequest request) {
        court.setName(request.name());
        court.setType(request.type());
        court.setDescription(request.description());
        court.setPricePerHour(request.pricePerHour());
        court.setImageUrl(request.imageUrl());
        court.setMaxPlayers(request.maxPlayers());
        court.setAmenities(request.amenities());
    }
}
