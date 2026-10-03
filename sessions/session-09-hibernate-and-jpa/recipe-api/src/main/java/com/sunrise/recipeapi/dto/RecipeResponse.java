package com.sunrise.recipeapi.dto;

import java.util.List;

public record RecipeResponse(
    String id, String title, String cuisine, int minutes, int servings, List<String> ingredients) {}
