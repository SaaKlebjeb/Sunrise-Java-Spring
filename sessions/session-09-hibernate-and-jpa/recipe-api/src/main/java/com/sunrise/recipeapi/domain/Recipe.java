package com.sunrise.recipeapi.domain;

import java.util.List;

public record Recipe(
        String id,
        String title,
        String cuisine,
        int minutes,
        int servings,
        List<String> ingredients,
        String internalNotes
) {
    public Recipe withId(String newId) {
        return new Recipe(newId, title, cuisine, minutes, servings, ingredients, internalNotes);
    }
}
