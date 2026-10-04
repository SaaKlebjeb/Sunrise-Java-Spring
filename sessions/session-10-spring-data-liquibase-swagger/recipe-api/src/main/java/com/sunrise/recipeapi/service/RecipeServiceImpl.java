package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {
  private final RecipeRepository recipeRepository;
  public static final Set<String> SORTABLE = Set.of("title", "minutes", "servings", "cuisine");
  public static final int MAX_SIZE = 50;

  @Override
  public List<Recipe> findAll(String cuisine) {
    return cuisine == null
        ? recipeRepository.findAll()
        : recipeRepository.findByCuisineIgnoreCase(cuisine);
  }

  @Override
  public Recipe findById(Long id) {
    return recipeRepository
        .findById(id)
        .orElseThrow(() -> new RecipeNotFoundException("recipe id: " + id + " not found"));
  }

  @Override
  @Transactional
  public Recipe create(Recipe recipe) {
    return recipeRepository.save(recipe);
  }

  @Override
  @Transactional
  public Recipe replace(Long id, Recipe recipe) {
    if (!recipeRepository.existsById(id)) {
      throw new RecipeNotFoundException("recipe id: " + id + " not found");
    }

    recipe.setId(id);

    return recipeRepository.save(recipe);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    recipeRepository.deleteById(id);
  }

  @Override
  public Page<Recipe> findAll(int page, int size, String sort) {
    String sortField = SORTABLE.contains(sort) ? sort : "id";
    int maxSize = Math.min(size, MAX_SIZE);
    Pageable pageable = PageRequest.of(page, maxSize, Sort.by(sortField));
    return recipeRepository.findAll(pageable);
  }

  @Override
  public List<Recipe> findByIngredient(String ingredient) {
    return ingredient != null
        ? recipeRepository.findByIngredient(ingredient)
        : recipeRepository.findByIngredient("");
  }
}
