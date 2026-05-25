package com.sportreserve.admin.dto;

public record LoginResponse(
    String token,
    String username,
    String role
) {}
