package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  @Override
  public AppUser getCurrentUser() {
    // The Authentication the JWT filter put in the SecurityContext
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // Its principal is the UserDetails the filter loaded -- our own AppUser
    return (AppUser) authentication.getPrincipal();
  }
}
