package com.sunrise.recipeapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateRecipeRequest(
    @NotBlank(message = "title can not be blank") String title,
    @NotBlank(message = "cuisine can not be blank") String cuisine,
    @Min(value = 1, message = "minutes must be at least 1") int minutes,
    @Min(value = 1, message = "servings must be at least 1") int servings,
    @NotEmpty(message = "ingredients must be at least 1")
        List<@NotBlank(message = "ingredient must not be blank") String> ingredients,
    String internalNotes) {}
