package com.sunrise.recipeapi.service;

public class EmailAlreadyExistException extends RuntimeException {
    EmailAlreadyExistException(String message) {
        super(message);
    }
}
