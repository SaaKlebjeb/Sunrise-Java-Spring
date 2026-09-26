package com.sunrise.recipes.service;

import com.sunrise.recipes.domain.Recipe;

import java.util.List;
import java.util.Optional;


public interface RecipeService {
    List<Recipe> findAll(String cuisine);
    Optional<Recipe> findById(String id);
}
