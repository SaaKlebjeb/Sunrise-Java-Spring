package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {
  private final RecipeRepository recipeRepository;

  @Override
  public List<Recipe> findAll(String cuisine) {
    return cuisine == null ? recipeRepository.findAll() : recipeRepository.findByCuisineIgnoreCase(cuisine);
  }

  @Override
  public Recipe findById(Long id) {
    return recipeRepository
        .findById(id)
        .orElseThrow(() -> new RecipeNotFoundException("recipe id: " + id + " not found"));
  }

  @Override
  public Recipe create(Recipe recipe) {
    return recipeRepository.save(recipe);
  }

  @Override
  public Recipe replace(Long id, Recipe recipe) {
    if (!recipeRepository.existsById(id)) {
      throw new RecipeNotFoundException("recipe id: " + id + " not found");
    }

    recipe.setId(id);

    return recipeRepository.save(recipe);
  }

  @Override
  public void delete(Long id) {
    recipeRepository.deleteById(id);
  }
}
