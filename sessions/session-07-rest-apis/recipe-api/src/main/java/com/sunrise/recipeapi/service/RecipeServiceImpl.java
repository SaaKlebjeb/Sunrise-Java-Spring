package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Service
public class RecipeServiceImpl implements RecipeService {
  private final RecipeRepository recipeRepository;

  public RecipeServiceImpl(RecipeRepository recipeRepository) {
    this.recipeRepository = recipeRepository;
  }

  @Override
  public List<Recipe> findAll(@RequestParam(value = "cuisine", defaultValue = "") String cuisine) {
    return cuisine == null ? recipeRepository.findAll() : recipeRepository.findByCuisine(cuisine);
  }

  @Override
  public Recipe findById(String id) {
    return recipeRepository
        .findById(id)
        .orElseThrow(() -> new RecipeNotFoundException("recipe id: " + id + " not found"));
  }

  @Override
  public Recipe create(Recipe recipe) {
    return recipeRepository.save(recipe);
  }

  @Override
  public void delete(String id) {
    recipeRepository.deleteById(id);
  }
}
