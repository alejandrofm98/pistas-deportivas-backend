package com.sportreserve.payment.dto;

public record PaymentConfirmResponse(
    boolean success,
    String order,
    String transactionId
) {}
