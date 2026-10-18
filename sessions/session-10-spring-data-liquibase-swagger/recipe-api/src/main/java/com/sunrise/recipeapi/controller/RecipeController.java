package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.*;
import com.sunrise.recipeapi.mapper.RecipeMapper;
import com.sunrise.recipeapi.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {
  private final RecipeService recipeService;
  private final RecipeMapper recipeMapper;

  @GetMapping
  public ResponseEntity<List<RecipeResponse>> listRecipes(String cuisine) {
    List<RecipeResponse> responses =
        recipeService.findAll(cuisine).stream().map(recipeMapper::toRecipeResponse).toList();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{id}")
  public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable Long id) {
    Recipe recipe = recipeService.findById(id);
    return ResponseEntity.ok(recipeMapper.toRecipeResponse(recipe));
  }

  @PostMapping
  public ResponseEntity<RecipeResponse> createRecipe(
      @Validated @RequestBody CreateRecipeRequest request) {
    Recipe savedRecipe = recipeService.create(recipeMapper.toRecipe(request));
    return ResponseEntity.created(URI.create("/recipes/" + savedRecipe.getId()))
        .body(recipeMapper.toRecipeResponse(savedRecipe));
  }

  // PUT replaces the whole recipe, so the body has the same shape as a create
  @PutMapping("/{id}")
  public ResponseEntity<RecipeResponse> updateRecipe(
      @PathVariable Long id, @Validated @RequestBody CreateRecipeRequest request) {
    Recipe updatedRecipe = recipeService.replace(id, recipeMapper.toRecipe(request));
    return ResponseEntity.ok(recipeMapper.toRecipeResponse(updatedRecipe));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
    recipeService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/page")
  public ResponseEntity<PageResponse<RecipeResponse>> getPageOfRecipe(
          PaginationRequest pagination) {
    Page<Recipe> recipePage = recipeService.findAll(pagination);
    PageResponse<RecipeResponse> response =
        new PageResponse<>(
            recipePage.getContent().stream().map(recipeMapper::toRecipeResponse).toList(),
            recipePage.getNumber(),
            recipePage.getSize(),
            recipePage.getTotalElements(),
            recipePage.getTotalPages());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/search")
  public ResponseEntity<List<RecipeResponse>> searchRecipe(@RequestParam String ingredient) {
    return ResponseEntity.ok(
        recipeService.findByIngredient(ingredient).stream()
            .map(recipeMapper::toRecipeResponse)
            .toList());
  }

  @GetMapping("/filter")
  public ResponseEntity<PageResponse<RecipeResponse>> filterRecipe(RecipeFilter filter, PaginationRequest pagination ) {
    Page<Recipe> recipePage = recipeService.filter(filter, pagination);
    PageResponse<RecipeResponse> response =
            new PageResponse<>(
                    recipePage.getContent().stream().map(recipeMapper::toRecipeResponse).toList(),
                    recipePage.getNumber(),
                    recipePage.getSize(),
                    recipePage.getTotalElements(),
                    recipePage.getTotalPages());
    return ResponseEntity.ok(response);
  }
}
