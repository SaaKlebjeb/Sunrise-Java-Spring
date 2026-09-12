package com.sunrise.bookshop;

import com.sunrise.bookshop.data.Catalogue;
import com.sunrise.bookshop.model.Book;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * REFERENCE SOLUTION — homework drill set E.
 */
public class AdvancedDrills {

    private final List<Book> books = Catalogue.books();

    public List<String> allTagsSorted() {
        return books.stream().flatMap(b -> b.tags().stream()).distinct().sorted().toList();
    }

    public String allTitlesJoined() {
        return books.stream().map(Book::title).collect(Collectors.joining(" | "));
    }

    public DoubleSummaryStatistics priceStatistics() {
        return books.stream().collect(Collectors.summarizingDouble(Book::price));
    }

    public Map<String, Integer> copiesSoldByAuthor() {
        return books.stream().collect(Collectors.toMap(Book::author, Book::copiesSold, Integer::sum));
    }

    public Map<Integer, List<String>> titlesByDecade() {
        return books.stream()
                .collect(Collectors.groupingBy(b -> (b.year() / 10) * 10, TreeMap::new,
                        Collectors.mapping(Book::title, Collectors.toList())));
    }

    public List<String> prolificAuthors() {
        return books.stream()
                .collect(Collectors.groupingBy(Book::author, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
