package com.sunrise.recipes.repository;

import com.sunrise.recipes.domain.Recipe;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository {
    List<Recipe> findAll();

    Optional<Recipe> findById(String id);

    List<Recipe> findByCuisine(String cuisine);

    Recipe save(Recipe recipe);

    boolean deleteById(String id);

}
