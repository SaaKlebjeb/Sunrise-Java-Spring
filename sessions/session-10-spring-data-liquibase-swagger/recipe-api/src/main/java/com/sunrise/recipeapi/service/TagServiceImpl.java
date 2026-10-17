package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.domain.Tag;
import com.sunrise.recipeapi.repository.RecipeRepository;
import com.sunrise.recipeapi.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
  private final TagRepository tagRepository;
  private final RecipeRepository recipeRepository;

  @Override
  public void addTag(Long recipeId, String name) {
    Recipe recipe = findById(recipeId);

    // Find existing or create tag
    Tag tag =
        tagRepository
            .findByNameIgnoreCase(name)
            .orElseGet(() -> tagRepository.save(new Tag(name.toLowerCase())));

    recipe.getTags().add(tag);
    recipeRepository.save(recipe);
  }

  @Override
  public List<String> tagsOf(Long recipeId) {
    Recipe recipe = findById(recipeId);
    return recipe.getTags().stream().map(Tag::getName).toList();
  }

  @Override
  public List<Recipe> recipesTagged(String name) {
    return recipeRepository.findAllByTagsNameIgnoreCase(name);
  }

  private Recipe findById(Long recipeId) {
    return recipeRepository
        .findById(recipeId)
        .orElseThrow(() -> new RecipeNotFoundException("recipe not found id= " + recipeId));
  }
}
