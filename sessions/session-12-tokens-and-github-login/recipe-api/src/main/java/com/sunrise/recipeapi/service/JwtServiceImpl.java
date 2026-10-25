package com.sunrise.recipeapi.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {
  private final long expirationMs;
  private final SecretKey signingKey;

  public JwtServiceImpl(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    // HS256 needs at least 32 bytes of key
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  @Override
  public String generateToken(UserDetails user) {
    String role = user.getAuthorities().iterator().next().getAuthority();
    Date now = new Date();
    return Jwts.builder()
        .issuer("recipe-api")
        .subject(user.getUsername())
        .claim("role", role)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(signingKey)
        .compact();
  }

  @Override
  public String extractUsername(String token) {
    return parse(token).getSubject();
  }

  @Override
  public long getExpiredIn() {
    return this.expirationMs;
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}
