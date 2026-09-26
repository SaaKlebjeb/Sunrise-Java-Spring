package com.sunrise.recipeapi.dto;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;

public record RecipeResponse(
    String id, String title, String cuisine, int minutes, int servings, List<String> ingredients) {
  public static RecipeResponse from(Recipe recipe) {
    return new RecipeResponse(
        recipe.id(),
        recipe.title(),
        recipe.cuisine(),
        recipe.minutes(),
        recipe.servings(),
        recipe.ingredients());
  }
}
