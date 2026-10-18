package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.domain.Tag;
import com.sunrise.recipeapi.dto.PaginationRequest;
import com.sunrise.recipeapi.dto.RecipeFilter;
import com.sunrise.recipeapi.repository.RecipeRepository;
import com.sunrise.recipeapi.spec.RecipeSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    Recipe existing = findById(id);

    // Set to old id
    recipe.setId(existing.getId());
    // Copy old tag
    Set<Tag> oldTags = new LinkedHashSet<>(existing.getTags());
    recipe.setTags(oldTags);

    return recipeRepository.save(recipe);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    recipeRepository.deleteById(id);
  }

  @Override
  public Page<Recipe> findAll(PaginationRequest pagination) {
    String sortField = SORTABLE.contains(pagination.getSort()) ? pagination.getSort() : "id";
    int maxSize = Math.min(pagination.getSize(), MAX_SIZE);
    Pageable pageable = PageRequest.of(pagination.getPage(), maxSize, Sort.by(sortField));
    return recipeRepository.findAll(pageable);
  }

  @Override
  public List<Recipe> findByIngredient(String ingredient) {
    return ingredient != null
        ? recipeRepository.findByIngredient(ingredient)
        : recipeRepository.findByIngredient("");
  }

  @Override
  public Page<Recipe> filter(RecipeFilter filter, PaginationRequest pagination) {
    String sortField = SORTABLE.contains(pagination.getSort()) ? pagination.getSort() : "id";
    int maxSize = Math.min(pagination.getSize(), MAX_SIZE);
    Pageable pageable = PageRequest.of(pagination.getPage(), maxSize, Sort.by(sortField));

    List<Specification<Recipe>> specs = new ArrayList<>();
    if (filter.getCuisine() != null) {
      specs.add(RecipeSpecifications.hasCuisine(filter.getCuisine()));
    }

    if (filter.getMinutes() != null) {
      specs.add(RecipeSpecifications.takesAtMost(filter.getMinutes()));
    }

    if (filter.getTagName() != null) {
      specs.add(RecipeSpecifications.hasTag(filter.getTagName()));
    }
    Specification<Recipe> specification = Specification.allOf(specs);

    return recipeRepository.findAll(specification, pageable);
  }
}
