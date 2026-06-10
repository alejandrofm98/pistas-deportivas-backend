package com.sportreserve.reservation.dto;

import com.sportreserve.court.dto.CourtResponse;
import com.sportreserve.payment.PaymentMethod;
import com.sportreserve.payment.PaymentStatus;
import com.sportreserve.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponse(
    UUID id,
    CourtResponse court,
    String customerName,
    String customerEmail,
    String customerPhone,
    LocalDate date,
    Double startTime,
    Double endTime,
    BigDecimal totalPrice,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    ReservationStatus status,
    LocalDateTime createdAt
) {}
