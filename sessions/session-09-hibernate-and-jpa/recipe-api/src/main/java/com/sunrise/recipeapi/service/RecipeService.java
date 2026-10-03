package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;

public interface RecipeService {
    List<Recipe> findAll(String cuisine);
    Recipe findById(String id);
    Recipe create(Recipe recipe);
    void delete(String id);
}
