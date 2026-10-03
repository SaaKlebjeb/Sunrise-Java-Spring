package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryRecipeRepository implements RecipeRepository {

    private final Map<String, Recipe> shelf = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @PostConstruct
    void seed() {
        save(new Recipe(null, "Fish Amok", "khmer", 45, 4,
                List.of("fish", "coconut milk", "kroeung"), "supplier: central market, pays late"));
        save(new Recipe(null, "Lok Lak", "khmer", 25, 2,
                List.of("beef", "lettuce", "lime", "kampot pepper"), "supplier: central market"));
        save(new Recipe(null, "Carbonara", "italian", 20, 2,
                List.of("spaghetti", "guanciale", "egg", "pecorino"), "supplier: deli, margin 12%"));
    }

    @Override
    public List<Recipe> findAll() {
        return new ArrayList<>(shelf.values());
    }

    @Override
    public Optional<Recipe> findById(String id) {
        return Optional.ofNullable(shelf.get(id));
    }

    @Override
    public List<Recipe> findByCuisine(String cuisine) {
        return shelf.values().stream()
                .filter(recipe -> recipe.cuisine().equalsIgnoreCase(cuisine))
                .toList();
    }

    @Override
    public Recipe save(Recipe recipe) {
        Recipe stored = recipe.id() == null
                ? recipe.withId(String.valueOf(nextId.getAndIncrement()))
                : recipe;
        shelf.put(stored.id(), stored);
        return stored;
    }

    @Override
    public boolean deleteById(String id) {
        return shelf.remove(id) != null;
    }
}