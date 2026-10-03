package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByCuisineIgnoreCase(String cuisine);
}
