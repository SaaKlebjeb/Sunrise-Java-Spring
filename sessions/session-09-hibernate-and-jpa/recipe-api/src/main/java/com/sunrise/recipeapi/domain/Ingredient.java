package com.sunrise.recipeapi.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
public class Ingredient {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name;

  @ManyToOne
  @JoinColumn(name = "recipe_id")
  private Recipe recipe;

  public Ingredient(String name, Recipe recipe) {
    this.name = name;
    this.recipe = recipe;
  }
}
