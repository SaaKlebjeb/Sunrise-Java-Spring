package com.sunrise.recipeapi.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateToken(UserDetails user);
    String extractUsername(String token);
    long getExpiredIn();
}
