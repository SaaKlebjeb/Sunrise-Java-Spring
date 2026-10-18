package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {
  List<Recipe> findByCuisineIgnoreCase(String cuisine);

  @Query("""
    select r from Recipe r
      join r.ingredients i
        where lower(i.name) = lower(:ingredient)
 """)
  List<Recipe> findByIngredient(@Param("ingredient") String ingredient);

  @EntityGraph(attributePaths = "ingredients")
  List<Recipe> findAll();

  List<Recipe> findAllByTagsNameIgnoreCase(String name);
}
