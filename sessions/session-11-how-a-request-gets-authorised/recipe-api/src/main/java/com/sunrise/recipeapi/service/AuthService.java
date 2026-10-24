package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.dto.AuthResponse;
import com.sunrise.recipeapi.dto.UserLoginRequest;
import com.sunrise.recipeapi.dto.UserRegisterRequest;
import com.sunrise.recipeapi.dto.UserResponse;

public interface AuthService {
  UserResponse register(UserRegisterRequest request);
  AuthResponse login(UserLoginRequest request);
}
