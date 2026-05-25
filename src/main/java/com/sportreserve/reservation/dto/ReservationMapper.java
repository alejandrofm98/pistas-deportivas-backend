package com.sportreserve.reservation.dto;

import com.sportreserve.court.dto.CourtMapper;
import com.sportreserve.reservation.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    private final CourtMapper courtMapper;

    public ReservationMapper(CourtMapper courtMapper) {
        this.courtMapper = courtMapper;
    }

    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            courtMapper.toResponse(reservation.getCourt()),
            reservation.getCustomerName(),
            reservation.getCustomerEmail(),
            reservation.getCustomerPhone(),
            reservation.getDate(),
            reservation.getStartTime(),
            reservation.getEndTime(),
            reservation.getTotalPrice(),
            reservation.getPaymentMethod(),
            reservation.getPaymentStatus(),
            reservation.getStatus(),
            reservation.getCreatedAt()
        );
    }
}
