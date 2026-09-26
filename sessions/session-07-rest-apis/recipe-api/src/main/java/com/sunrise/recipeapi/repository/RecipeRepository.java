package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository {
    List<Recipe> findAll();
    Optional<Recipe> findById(String id);
    List<Recipe> findByCuisine(String cuisine);
    Recipe save(Recipe recipe);
    boolean deleteById(String id);
}
