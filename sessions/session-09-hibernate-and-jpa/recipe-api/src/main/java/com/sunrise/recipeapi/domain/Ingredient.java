package com.sunrise.recipeapi.domain;

import lombok.*;

@Data
public class Ingredient {
    private String name;
    private String type;
    private double amount;
}
