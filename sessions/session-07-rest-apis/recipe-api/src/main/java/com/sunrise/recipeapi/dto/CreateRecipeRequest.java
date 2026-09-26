package com.sunrise.recipeapi.dto;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;

public record CreateRecipeRequest(
    String title,
    String cuisine,
    int minutes,
    int servings,
    List<String> ingredients,
    String internalNotes) {
  public Recipe toRecipe() {
    return new Recipe(null, title, cuisine, minutes, servings, ingredients, internalNotes);
  }
}
