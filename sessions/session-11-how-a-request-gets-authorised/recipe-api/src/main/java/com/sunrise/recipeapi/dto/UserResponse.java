package com.sunrise.recipeapi.dto;

import com.sunrise.recipeapi.enums.Role;

public record UserResponse(String fullName, String email, Role role) {}
