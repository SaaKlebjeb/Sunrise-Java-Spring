package com.sunrise.recipeapi.dto;

import lombok.Data;

@Data
public class RecipeFilter {
    private String tagName;
    private Integer minutes;
    private String cuisine;


}
