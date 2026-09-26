package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.CreateRecipeRequest;
import com.sunrise.recipeapi.dto.RecipeResponse;
import com.sunrise.recipeapi.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/recipes")
public class RecipeController {
  private final RecipeService recipeService;

  public RecipeController(RecipeService recipeService) {
    this.recipeService = recipeService;
  }

  @GetMapping
  public ResponseEntity<List<RecipeResponse>> listRecipes(String cuisine) {
    List<RecipeResponse> responses =
        recipeService.findAll(cuisine).stream().map(RecipeResponse::from).toList();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{id}")
  public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable String id) {
    Optional<Recipe> recipe = recipeService.findById(id);

    if (recipe.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(RecipeResponse.from(recipe.get()));
  }

  @PostMapping
  public ResponseEntity<RecipeResponse> createRecipe(@RequestBody CreateRecipeRequest request) {
    Recipe savedRecipe = recipeService.create(request.toRecipe());
    return ResponseEntity.created(URI.create("/recipes" + savedRecipe.id())).body(RecipeResponse.from(savedRecipe));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteRecipe(@PathVariable String id) {
    recipeService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
