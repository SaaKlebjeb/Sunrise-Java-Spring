package com.sunrise.recipeapi.service;

public class RecipeNotFoundException extends RuntimeException {
    RecipeNotFoundException(String message) {
        super(message);
    }
}
