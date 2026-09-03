package com.sunrise.bookshop.model;

import java.util.List;

/**
 * One title in the shop's catalogue.
 *
 * DO NOT MODIFY.
 *
 * Note the compact constructor: it copies the tag list, so nobody can
 * reach in and edit a Book after it exists. That is the shallow
 * immutability trap from the previous session, already closed.
 */
public record Book(
        String title,
        String author,
        String genre,
        double price,
        int year,
        int copiesSold,
        List<String> tags) {

    public Book {
        tags = List.copyOf(tags);
    }
}
