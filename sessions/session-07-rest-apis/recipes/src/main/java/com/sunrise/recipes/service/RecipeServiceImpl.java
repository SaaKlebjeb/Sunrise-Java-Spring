package com.sunrise.recipes.service;

import com.sunrise.recipes.domain.Recipe;
import com.sunrise.recipes.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RecipeServiceImpl implements RecipeService{

    private final RecipeRepository recipeRepository;

    RecipeServiceImpl(RecipeRepository recipeRepository){
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<Recipe> findAll(String cuisine) {
        return cuisine == null? recipeRepository.findAll() : recipeRepository.findByCuisine(cuisine);
    }

    @Override
    public Optional<Recipe> findById(String id) {
        return recipeRepository.findById(id);
    }
}
