package com.sunrise.recipeapi.dto;

import java.util.List;

public record RecipeResponse(
    Long id, String title, String cuisine, int minutes, int servings, List<String> ingredients) {}
