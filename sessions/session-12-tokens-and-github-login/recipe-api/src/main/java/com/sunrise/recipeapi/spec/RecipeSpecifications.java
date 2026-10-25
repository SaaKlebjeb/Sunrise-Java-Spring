package com.sunrise.recipeapi.spec;

import com.sunrise.recipeapi.domain.Recipe;
import org.springframework.data.jpa.domain.Specification;

public final class RecipeSpecifications {
  private RecipeSpecifications() {}

  public static Specification<Recipe> hasCuisine(String cuisine) {
    return (root, query, cb) -> cb.equal(cb.lower(root.get("cuisine")), cuisine.toLowerCase());
  }

  public static Specification<Recipe> takesAtMost(int minutes) {
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("minutes"), minutes);
  }

  public static Specification<Recipe> hasTag(String name) {
    return (root, query, cb) ->
        cb.equal(cb.lower(root.join("tags").get("name")), name.toLowerCase());
  }
}
