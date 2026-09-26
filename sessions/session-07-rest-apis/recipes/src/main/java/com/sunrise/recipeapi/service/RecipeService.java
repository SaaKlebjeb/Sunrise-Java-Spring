package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;
import java.util.Optional;

public interface RecipeService {
    List<Recipe> findAll(String cuisine);
    Optional<Recipe> findById(String id);
    Recipe create(Recipe recipe);
    void delete(String id);
}
