package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface RecipeService {
    List<Recipe> findAll(String cuisine);
    Recipe findById(Long id);
    Recipe create(Recipe recipe);
    Recipe replace(Long id, Recipe recipe);
    void delete(Long id);
    Page<Recipe> findAll(int page, int size, String sort);
    List<Recipe> findByIngredient(String ingredient);
}
