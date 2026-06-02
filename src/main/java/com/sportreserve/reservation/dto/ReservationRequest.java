package com.sportreserve.reservation.dto;

import com.sportreserve.payment.PaymentMethod;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationRequest(
    @NotNull UUID courtId,
    @NotBlank String customerName,
    @NotBlank @Email String customerEmail,
    String customerPhone,
    @NotNull LocalDate date,
    @NotNull @Min(7) @Max(23) Double startTime,
    @NotNull PaymentMethod paymentMethod,
    Double endTime
) {}
