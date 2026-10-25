package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.dto.UserResponse;
import com.sunrise.recipeapi.mapper.UserMapper;
import com.sunrise.recipeapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final UserMapper userMapper;

  // Who is signed in? The service knows -- the controller only speaks HTTP
  @GetMapping("/me")
  public ResponseEntity<UserResponse> me() {
    return ResponseEntity.ok(userMapper.toUserResponse(userService.getCurrentUser()));
  }
}
