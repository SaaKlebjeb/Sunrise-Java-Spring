package com.sunrise.bookshop.data;

import com.sunrise.bookshop.model.Book;

import java.util.List;

/**
 * The catalogue every drill runs against.
 *
 * DO NOT MODIFY — the tests know these numbers by heart.
 */
public final class Catalogue {

    private Catalogue() {
    }

    public static List<Book> books() {
        return List.of(
                new Book("The Silent Harbour", "Sokha Meas", "Fiction",
                        12.50, 2018, 4200, List.of("bestseller", "coastal")),
                new Book("Mekong Mornings", "Sokha Meas", "Fiction",
                        9.99, 2021, 6100, List.of("bestseller", "river", "coastal")),
                new Book("Concrete and Rain", "Dara Chan", "Fiction",
                        18.00, 2015, 1800, List.of("literary")),
                new Book("Building Web APIs", "Vichea Ly", "Technical",
                        45.00, 2022, 900, List.of("programming", "web")),
                new Book("Databases in Practice", "Vichea Ly", "Technical",
                        52.00, 2019, 1500, List.of("programming", "data")),
                new Book("The Patient Gardener", "Bopha Sen", "Non-fiction",
                        14.25, 2020, 3300, List.of("gardening", "slow-living")),
                new Book("Kitchen Notes", "Bopha Sen", "Non-fiction",
                        11.00, 2023, 5400, List.of("cooking", "bestseller")),
                new Book("A History of Angkor", "Rithy Pen", "Non-fiction",
                        29.99, 2011, 2100, List.of("history")),
                new Book("Night Market", "Dara Chan", "Fiction",
                        8.50, 2024, 7300, List.of("bestseller", "urban")),
                new Book("Clean Java", "Vichea Ly", "Technical",
                        39.50, 2024, 2600, List.of("programming", "java")));
    }
}
