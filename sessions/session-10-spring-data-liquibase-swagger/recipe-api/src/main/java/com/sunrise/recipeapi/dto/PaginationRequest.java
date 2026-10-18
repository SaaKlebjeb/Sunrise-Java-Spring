package com.sunrise.recipeapi.dto;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

@Data
public class PaginationRequest {
  private int page = 0;
  private int size = 10;
  private String sort = "id";
}
