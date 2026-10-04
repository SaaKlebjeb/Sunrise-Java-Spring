package com.sunrise.recipeapi.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, length = 100)
  private String cuisine;

  private int minutes;
  private int servings;


  @Column(name = "internal_notes")
  private String internalNotes;

  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Ingredient> ingredients = new ArrayList<>();

  public void addIngredient(String name) {
    this.ingredients.add(new Ingredient(name, this));
  }
}
