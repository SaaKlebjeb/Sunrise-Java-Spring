package com.sunrise.bookshop;

import com.sunrise.bookshop.data.Catalogue;
import com.sunrise.bookshop.model.Book;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** REFERENCE SOLUTION — homework drill set D. */
public class GroupingDrills {

    private final List<Book> books = Catalogue.books();

    public Map<String, List<Book>> booksByGenre() {
        return books.stream().collect(Collectors.groupingBy(Book::genre));
    }

    public Map<String, Long> titleCountByGenre() {
        return books.stream().collect(Collectors.groupingBy(Book::genre, Collectors.counting()));
    }

    public Map<String, Double> averagePriceByGenre() {
        return books.stream()
                .collect(Collectors.groupingBy(Book::genre, Collectors.averagingDouble(Book::price)));
    }

    public Map<String, List<String>> sortedTitlesByGenre() {
        return books.stream()
                .sorted(Comparator.comparing(Book::title))
                .collect(Collectors.groupingBy(Book::genre,
                        Collectors.mapping(Book::title, Collectors.toList())));
    }

    public Map<Boolean, List<String>> titlesUnderFifteen() {
        return books.stream()
                .collect(Collectors.partitioningBy(b -> b.price() < 15.00,
                        Collectors.mapping(Book::title, Collectors.toList())));
    }

    public Map<String, String> bestSellerByGenre() {
        return books.stream()
                .collect(Collectors.groupingBy(Book::genre,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(Book::copiesSold)),
                                best -> best.orElseThrow().title())));
    }
}
