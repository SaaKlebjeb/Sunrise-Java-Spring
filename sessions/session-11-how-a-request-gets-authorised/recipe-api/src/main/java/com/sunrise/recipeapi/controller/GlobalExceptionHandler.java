package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.service.RecipeNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  @ExceptionHandler(RecipeNotFoundException.class)
  ProblemDetail handleRecipeNotFound(RecipeNotFoundException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
    problemDetail.setType(URI.create("recipe-api.sunrise.com/docs/RecipeNotFound"));
    problemDetail.setTitle("Recipe not found");
    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getFieldErrors().forEach(field -> errors.put(field.getField(), field.getDefaultMessage()));
    ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
    problemDetail.setType(URI.create("recipe-api.sunrise.com/docs/MethodArgumentNotValidException"));
    problemDetail.setTitle("Validation failed");
    problemDetail.setDetail(ex.getFieldErrors().size() + " fields not valid");
    problemDetail.setProperty("errors", errors);
    return problemDetail;
  }
}
