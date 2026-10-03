package com.sunrise.recipeapi.mapper;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.CreateRecipeRequest;
import com.sunrise.recipeapi.dto.RecipeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RecipeMapper {
    RecipeResponse toRecipeResponse(Recipe recipe);
    Recipe toRecipe(CreateRecipeRequest request);
}
