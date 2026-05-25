package com.sportreserve.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID reservationId,
    String redsysOrder,
    BigDecimal amount,
    String status,
    String redsysTransactionId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
