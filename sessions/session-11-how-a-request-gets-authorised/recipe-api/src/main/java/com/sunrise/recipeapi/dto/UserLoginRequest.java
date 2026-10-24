package com.sunrise.recipeapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @Email(message = "email must be valid")
        String email,
        @NotBlank(message = "password must not be blank")
        String password
) {}
