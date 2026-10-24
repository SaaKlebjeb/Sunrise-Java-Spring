package com.sunrise.recipeapi.dto;

public record AuthResponse(String token, String tokenType, long expiresIn, String email) {}