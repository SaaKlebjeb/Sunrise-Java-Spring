package com.sunrise.bookshop;

import com.sunrise.bookshop.data.Catalogue;
import com.sunrise.bookshop.model.Book;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * HOMEWORK — DRILL SET D: turning a list into a Map
 * ============================================================
 * Same rule as in class: the loop above each method already
 * works. Replace it with a collector.
 *
 * groupingBy(key)                  → Map<K, List<T>>
 * groupingBy(key, downstream)      → Map<K, whatever the downstream makes>
 * partitioningBy(predicate)        → Map<Boolean, List<T>>
 *
 * The "downstream" collector is the one worth slowing down for.
 * It is what turns a Map of lists into a Map of counts, averages,
 * names, or bests.
 *
 * Run GroupingDrillsTest to check your answers.
 * ============================================================
 */
public class GroupingDrills {

    private final List<Book> books = Catalogue.books();

    // --------------------------------------------------------
    // D1 — the books in each genre
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Map<String, List<Book>> result = new HashMap<>();
    //   for (Book b : books) {
    //       result.computeIfAbsent(b.genre(), k -> new ArrayList<>()).add(b);
    //   }
    //   return result;
    // --------------------------------------------------------
    public Map<String, List<Book>> booksByGenre() {
        // TODO: rewrite with Collectors.groupingBy
        return null;
    }

    // --------------------------------------------------------
    // D2 — how many books in each genre
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Map<String, Long> result = new HashMap<>();
    //   for (Book b : books) {
    //       result.merge(b.genre(), 1L, Long::sum);
    //   }
    //   return result;
    //
    // groupingBy with Collectors.counting() as the downstream.
    // --------------------------------------------------------
    public Map<String, Long> titleCountByGenre() {
        // TODO: rewrite with Collectors.groupingBy
        return null;
    }

    // --------------------------------------------------------
    // D3 — the average price in each genre
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Map<String, List<Double>> prices = new HashMap<>();
    //   for (Book b : books) {
    //       prices.computeIfAbsent(b.genre(), k -> new ArrayList<>()).add(b.price());
    //   }
    //   Map<String, Double> result = new HashMap<>();
    //   for (var entry : prices.entrySet()) {
    //       double total = 0;
    //       for (double p : entry.getValue()) total += p;
    //       result.put(entry.getKey(), total / entry.getValue().size());
    //   }
    //   return result;
    //
    // Eleven lines and a temporary Map, or one collector with
    // Collectors.averagingDouble(...) downstream.
    // --------------------------------------------------------
    public Map<String, Double> averagePriceByGenre() {
        // TODO: rewrite with Collectors.groupingBy
        return null;
    }

    // --------------------------------------------------------
    // D4 — just the titles in each genre, sorted A to Z
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Map<String, List<String>> result = new HashMap<>();
    //   for (Book b : books) {
    //       result.computeIfAbsent(b.genre(), k -> new ArrayList<>()).add(b.title());
    //   }
    //   for (List<String> titles : result.values()) {
    //       Collections.sort(titles);
    //   }
    //   return result;
    //
    // Collectors.mapping(...) changes what lands in each group.
    // For the sort, look at Collectors.collectingAndThen, or sort
    // the stream before you collect it — both work, and it is
    // worth knowing why.
    // --------------------------------------------------------
    public Map<String, List<String>> sortedTitlesByGenre() {
        // TODO: rewrite with Collectors.groupingBy
        return null;
    }

    // --------------------------------------------------------
    // D5 — split the catalogue into cheap (under 15.00) and not
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   List<String> cheap = new ArrayList<>();
    //   List<String> rest = new ArrayList<>();
    //   for (Book b : books) {
    //       if (b.price() < 15.00) cheap.add(b.title());
    //       else rest.add(b.title());
    //   }
    //   Map<Boolean, List<String>> result = new HashMap<>();
    //   result.put(true, cheap);
    //   result.put(false, rest);
    //   return result;
    //
    // partitioningBy always returns both keys, true and false,
    // even when one side is empty. groupingBy would not.
    // Titles in catalogue order.
    // --------------------------------------------------------
    public Map<Boolean, List<String>> titlesUnderFifteen() {
        // TODO: rewrite with Collectors.partitioningBy
        return null;
    }

    // --------------------------------------------------------
    // D6 — the best-selling title in each genre
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Map<String, Book> best = new HashMap<>();
    //   for (Book b : books) {
    //       Book current = best.get(b.genre());
    //       if (current == null || b.copiesSold() > current.copiesSold()) {
    //           best.put(b.genre(), b);
    //       }
    //   }
    //   Map<String, String> result = new HashMap<>();
    //   for (var entry : best.entrySet()) {
    //       result.put(entry.getKey(), entry.getValue().title());
    //   }
    //   return result;
    //
    // Collectors.maxBy(...) downstream gives you an Optional per
    // group. Collectors.collectingAndThen(...) unwraps it. This
    // is the hardest drill in the set — expect to look it up.
    // --------------------------------------------------------
    public Map<String, String> bestSellerByGenre() {
        // TODO: rewrite with Collectors.groupingBy
        return null;
    }
}
