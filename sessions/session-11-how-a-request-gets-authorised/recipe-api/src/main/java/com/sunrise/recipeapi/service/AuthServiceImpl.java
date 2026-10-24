package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.AppUser;
import com.sunrise.recipeapi.dto.AuthResponse;
import com.sunrise.recipeapi.dto.UserLoginRequest;
import com.sunrise.recipeapi.dto.UserRegisterRequest;
import com.sunrise.recipeapi.dto.UserResponse;
import com.sunrise.recipeapi.enums.Role;
import com.sunrise.recipeapi.mapper.UserMapper;
import com.sunrise.recipeapi.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final AppUserRepository appUserRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  @Override
  public UserResponse register(UserRegisterRequest request) {
    boolean emailExist = appUserRepository.existsByEmail(request.email());

    if (emailExist) {
      throw new EmailAlreadyExistException("email already exist");
    }

    AppUser user = userMapper.toAppUser(request);

    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(Role.USER);
    AppUser savedUser = appUserRepository.save(user);

    return userMapper.toUserResponse(savedUser);
  }

  @Override
  public AuthResponse login(UserLoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    UserDetails user = (UserDetails) authentication.getPrincipal();
    String token = jwtService.generateToken(user);

    return new AuthResponse(token, "Bearer", jwtService.getExpiredIn(), user.getUsername());
  }
}
