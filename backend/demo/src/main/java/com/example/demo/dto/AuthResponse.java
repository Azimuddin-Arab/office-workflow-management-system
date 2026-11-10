package com.example.demo.dto;

public record AuthResponse(
    String token,
    String tokenType,
    Long id,
    String fullName,
    String email,
    String role
) {}