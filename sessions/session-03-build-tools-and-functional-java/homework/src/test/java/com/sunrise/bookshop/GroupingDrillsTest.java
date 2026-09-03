package com.sunrise.bookshop;

import com.sunrise.bookshop.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HOMEWORK SET D — 6 checks.
 *
 * DO NOT MODIFY.
 */
@DisplayName("Set D — Grouping")
class GroupingDrillsTest {

    private GroupingDrills drills;

    @BeforeEach
    void setUp() {
        drills = new GroupingDrills();
    }

    @Test
    @DisplayName("D1 · books grouped by genre")
    void booksByGenre() {
        Map<String, List<Book>> result = drills.booksByGenre();
        assertNotNull(result, "still returning null — write the collector");
        assertEquals(3, result.size(), "three genres");
        assertEquals(4, result.get("Fiction").size());
        assertEquals(3, result.get("Non-fiction").size());
        assertEquals(3, result.get("Technical").size());
    }

    @Test
    @DisplayName("D2 · how many titles per genre")
    void titleCountByGenre() {
        Map<String, Long> result = drills.titleCountByGenre();
        assertNotNull(result, "still returning null — write the collector");
        assertEquals(Map.of("Fiction", 4L, "Non-fiction", 3L, "Technical", 3L), result);
    }

    @Test
    @DisplayName("D3 · average price per genre")
    void averagePriceByGenre() {
        Map<String, Double> result = drills.averagePriceByGenre();
        assertNotNull(result, "still returning null — write the collector");
        assertEquals(3, result.size());
        assertEquals(12.2475, result.get("Fiction"), 0.0001);
        assertEquals(18.4133, result.get("Non-fiction"), 0.0001);
        assertEquals(45.5, result.get("Technical"), 0.0001);
    }

    @Test
    @DisplayName("D4 · titles per genre, sorted A to Z")
    void sortedTitlesByGenre() {
        Map<String, List<String>> result = drills.sortedTitlesByGenre();
        assertNotNull(result, "still returning null — write the collector");
        assertEquals(List.of("Concrete and Rain", "Mekong Mornings",
                        "Night Market", "The Silent Harbour"),
                result.get("Fiction"));
        assertEquals(List.of("A History of Angkor", "Kitchen Notes",
                        "The Patient Gardener"),
                result.get("Non-fiction"));
        assertEquals(List.of("Building Web APIs", "Clean Java",
                        "Databases in Practice"),
                result.get("Technical"));
    }

    @Test
    @DisplayName("D5 · partitioned into under 15.00 and the rest")
    void titlesUnderFifteen() {
        Map<Boolean, List<String>> result = drills.titlesUnderFifteen();
        assertNotNull(result, "still returning null — write the collector");
        assertTrue(result.containsKey(true) && result.containsKey(false),
                "partitioningBy always gives you both keys");
        assertEquals(List.of("The Silent Harbour", "Mekong Mornings",
                        "The Patient Gardener", "Kitchen Notes", "Night Market"),
                result.get(true));
        assertEquals(List.of("Concrete and Rain", "Building Web APIs",
                        "Databases in Practice", "A History of Angkor", "Clean Java"),
                result.get(false));
    }

    @Test
    @DisplayName("D6 · the best-selling title in each genre")
    void bestSellerByGenre() {
        Map<String, String> result = drills.bestSellerByGenre();
        assertNotNull(result, "still returning null — write the collector");
        assertEquals(Map.of(
                "Fiction", "Night Market",
                "Non-fiction", "Kitchen Notes",
                "Technical", "Clean Java"), result);
    }
}
