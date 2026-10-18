package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;

public interface TagService {
    /**
     * Create tag, then link to recipe
     * If the tag does not exist, create new
     * @param recipeId id of recipe
     * @param name name of tag
     */
    void addTag(Long recipeId, String name);

    /**
     * Get all tags from a recipe
     * @param recipeId id of recipe
     * @return list of tags as string
     */
    List<String> tagsOf(Long recipeId);

    /**
     * Get all recipe by tag name
     * @param name name of tag
     * @return list of recipes
     */
    List<Recipe> recipesTagged(String name);
}
