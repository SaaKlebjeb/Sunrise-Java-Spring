package com.sunrise.recipeapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRegisterRequest(
    @NotBlank(message = "full name not must be blank") String fullName,
    @Email(message = "email must be valid") String email,
    @NotBlank(message = "password must not be blank") String password) {}
