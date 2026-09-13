package com.sunrise.catalogueservice.domain;

public record CatalogueEntry(String isbn, String title, String author, int timesBorrowed) { }
