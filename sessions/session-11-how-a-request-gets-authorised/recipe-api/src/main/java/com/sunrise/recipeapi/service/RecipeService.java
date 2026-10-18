package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.PaginationRequest;
import com.sunrise.recipeapi.dto.RecipeFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface RecipeService {
    List<Recipe> findAll(String cuisine);
    Recipe findById(Long id);
    Recipe create(Recipe recipe);
    Recipe replace(Long id, Recipe recipe);
    void delete(Long id);
    Page<Recipe> findAll(PaginationRequest pagination);
    List<Recipe> findByIngredient(String ingredient);
    Page<Recipe> filter(RecipeFilter filter, PaginationRequest pagination);
}
