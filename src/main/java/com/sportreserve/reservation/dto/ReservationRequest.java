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
    @NotNull @Min(0) @Max(23) Integer startTime,
    @NotNull @Min(1) @Max(24) Integer endTime,
    @NotNull PaymentMethod paymentMethod
) {}
