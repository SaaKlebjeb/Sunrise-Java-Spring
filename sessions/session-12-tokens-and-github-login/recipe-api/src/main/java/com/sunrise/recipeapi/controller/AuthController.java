package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.dto.AuthResponse;
import com.sunrise.recipeapi.dto.UserLoginRequest;
import com.sunrise.recipeapi.dto.UserRegisterRequest;
import com.sunrise.recipeapi.dto.UserResponse;
import com.sunrise.recipeapi.service.AuthService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  ResponseEntity<UserResponse> register(@Validated @RequestBody UserRegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  @PostMapping("/login")
  ResponseEntity<AuthResponse> login(@Validated @RequestBody UserLoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }
}
