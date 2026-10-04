package com.sunrise.recipeapi.mapper;

import com.sunrise.recipeapi.domain.Ingredient;
import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.CreateRecipeRequest;
import com.sunrise.recipeapi.dto.RecipeResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RecipeMapper {
    RecipeResponse toRecipeResponse(Recipe recipe);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    Recipe toRecipe(CreateRecipeRequest request);

    @AfterMapping
    default void addIngredients(CreateRecipeRequest request, @MappingTarget Recipe recipe) {
        request.ingredients().forEach(recipe::addIngredient);
    }

    default String toName(Ingredient ingredient) {
        return ingredient.getName();
    }
}
