package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Ingredient;
import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.CreateRecipeRequest;
import com.sunrise.recipeapi.dto.RecipeResponse;
import com.sunrise.recipeapi.mapper.RecipeMapper;
import com.sunrise.recipeapi.mapper.RecipeMapperImpl;
import com.sunrise.recipeapi.repository.RecipeRepository;
import com.sunrise.recipeapi.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

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
  public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable String id) {
    Recipe recipe = recipeService.findById(id);
    return ResponseEntity.ok(recipeMapper.toRecipeResponse(recipe));
  }

  @PostMapping
  public ResponseEntity<RecipeResponse> createRecipe(
      @Validated @RequestBody CreateRecipeRequest request) {
    Recipe savedRecipe = recipeService.create(recipeMapper.toRecipe(request));
    return ResponseEntity.created(URI.create("/recipes/" + savedRecipe.id()))
        .body(recipeMapper.toRecipeResponse(savedRecipe));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteRecipe(@PathVariable String id) {
    recipeService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
