package com.sportreserve.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentConfirmRequest(
    @NotBlank String dsMerchantParameters,
    @NotBlank String dsSignature
) {}
