package com.sunrise.bookshop;

import com.sunrise.bookshop.data.Catalogue;
import com.sunrise.bookshop.model.Book;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * ============================================================
 * HOMEWORK — DRILL SET E: flattening, joining, summarising
 * ============================================================
 * flatMap        → many lists become one stream
 * joining        → one String, separators handled
 * summarizing*   → count, sum, min, average and max in one pass
 * toMap          → a Map with no grouping, and a rule for clashes
 *
 * Run AdvancedDrillsTest to check your answers.
 * ============================================================
 */
public class AdvancedDrills {

    private final List<Book> books = Catalogue.books();

    // --------------------------------------------------------
    // E1 — every tag used anywhere, no repeats, A to Z
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   List<String> result = new ArrayList<>();
    //   for (Book b : books) {
    //       for (String tag : b.tags()) {
    //           if (!result.contains(tag)) result.add(tag);
    //       }
    //   }
    //   Collections.sort(result);
    //   return result;
    //
    // Two nested loops. map(Book::tags) would give you a stream
    // of lists; flatMap(t -> t.tags().stream()) gives you a
    // stream of tags. That is the whole difference.
    // --------------------------------------------------------
    public List<String> allTagsSorted() {
        // TODO: rewrite with flatMap
        return null;
    }

    // --------------------------------------------------------
    // E2 — every title in one String: "A | B | C"
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   StringBuilder sb = new StringBuilder();
    //   for (Book b : books) {
    //       if (sb.length() > 0) sb.append(" | ");
    //       sb.append(b.title());
    //   }
    //   return sb.toString();
    //
    // Titles in catalogue order.
    // --------------------------------------------------------
    public String allTitlesJoined() {
        // TODO: rewrite with Collectors.joining
        return null;
    }

    // --------------------------------------------------------
    // E3 — count, min, max, sum and average price, in one pass
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   int count = 0; double sum = 0;
    //   double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
    //   for (Book b : books) {
    //       count++;
    //       sum += b.price();
    //       if (b.price() < min) min = b.price();
    //       if (b.price() > max) max = b.price();
    //   }
    //   ... and now build the summary object by hand.
    //
    // DoubleSummaryStatistics already is that object.
    // --------------------------------------------------------
    public DoubleSummaryStatistics priceStatistics() {
        // TODO: rewrite with Collectors.summarizingDouble
        return null;
    }

    // --------------------------------------------------------
    // E4 — total copies sold per author
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Map<String, Integer> result = new HashMap<>();
    //   for (Book b : books) {
    //       result.merge(b.author(), b.copiesSold(), Integer::sum);
    //   }
    //   return result;
    //
    // Collectors.toMap takes a key, a value, and — because two
    // books share an author — a third argument saying what to do
    // when a key turns up twice. Leave that third argument out
    // and you get IllegalStateException: Duplicate key. Try it
    // once on purpose; it is a very common production bug.
    // --------------------------------------------------------
    public Map<String, Integer> copiesSoldByAuthor() {
        // TODO: rewrite with Collectors.toMap
        return null;
    }

    // --------------------------------------------------------
    // E5 — titles grouped by the decade they were published in
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Map<Integer, List<String>> result = new TreeMap<>();
    //   for (Book b : books) {
    //       int decade = (b.year() / 10) * 10;
    //       result.computeIfAbsent(decade, k -> new ArrayList<>()).add(b.title());
    //   }
    //   return result;
    //
    // The key is computed, not a field. groupingBy takes any
    // function. Return a TreeMap so the decades come out in
    // order — groupingBy has a three-argument form that lets you
    // choose the Map implementation.
    // --------------------------------------------------------
    public Map<Integer, List<String>> titlesByDecade() {
        // TODO: rewrite with Collectors.groupingBy
        return null;
    }

    // --------------------------------------------------------
    // E6 — the authors with more than one book, A to Z
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Map<String, Integer> counts = new HashMap<>();
    //   for (Book b : books) {
    //       counts.merge(b.author(), 1, Integer::sum);
    //   }
    //   List<String> result = new ArrayList<>();
    //   for (var entry : counts.entrySet()) {
    //       if (entry.getValue() > 1) result.add(entry.getKey());
    //   }
    //   Collections.sort(result);
    //   return result;
    //
    // Two passes, and the second one streams a Map. entrySet()
    // .stream() is how you get there — a Map is not a stream
    // source on its own.
    // --------------------------------------------------------
    public List<String> prolificAuthors() {
        // TODO: group, then stream the resulting map
        return null;
    }
}
