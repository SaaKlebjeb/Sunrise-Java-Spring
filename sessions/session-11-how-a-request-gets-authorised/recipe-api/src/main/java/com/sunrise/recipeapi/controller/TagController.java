package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.dto.RecipeResponse;
import com.sunrise.recipeapi.mapper.RecipeMapper;
import com.sunrise.recipeapi.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController {

  private final TagService tagService;
  private final RecipeMapper recipeMapper;

  /** PUT, not POST: tagging the same recipe twice leaves it tagged once. */
  @PutMapping("/recipes/{id}/tags/{name}")
  public ResponseEntity<Void> addTag(@PathVariable Long id, @PathVariable String name) {
    tagService.addTag(id, name);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/recipes/{id}/tags")
  public List<String> tagsOf(@PathVariable Long id) {
    return tagService.tagsOf(id);
  }

  @GetMapping("/tags/{name}/recipes")
  public List<RecipeResponse> recipesTagged(@PathVariable String name) {
    return tagService.recipesTagged(name).stream().map(recipeMapper::toRecipeResponse).toList();
  }
}
