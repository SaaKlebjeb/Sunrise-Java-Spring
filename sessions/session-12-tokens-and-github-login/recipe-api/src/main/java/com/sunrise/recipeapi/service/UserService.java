package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.AppUser;

public interface UserService {
    /**
     * Get the signed-in user
     * Reads the AppUser the JWT filter put in the SecurityContext
     * @return the signed-in user
     */
    AppUser getCurrentUser();
}
