package com.sportreserve.payment.dto;

public record PaymentInitiateResponse(
    String url,
    String dsSignatureVersion,
    String dsMerchantParameters,
    String dsSignature
) {}
