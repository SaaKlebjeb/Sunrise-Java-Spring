package com.sunrise.recipeapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;

public record RecipeResponse(
    String id, String title, String cuisine, int minutes, int servings, List<String> ingredients) {}
