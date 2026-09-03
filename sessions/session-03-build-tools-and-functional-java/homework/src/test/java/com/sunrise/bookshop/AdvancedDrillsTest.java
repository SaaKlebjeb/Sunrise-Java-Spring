package com.sunrise.bookshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * HOMEWORK SET E — 6 checks.
 *
 * DO NOT MODIFY.
 */
@DisplayName("Set E — Flattening, joining, summarising")
class AdvancedDrillsTest {

    private AdvancedDrills drills;

    @BeforeEach
    void setUp() {
        drills = new AdvancedDrills();
    }

    @Test
    @DisplayName("E1 · every tag, no repeats, A to Z")
    void allTagsSorted() {
        var result = drills.allTagsSorted();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(List.of("bestseller", "coastal", "cooking", "data", "gardening",
                "history", "java", "literary", "programming", "river",
                "slow-living", "urban", "web"), result);
    }

    @Test
    @DisplayName("E2 · every title in one String")
    void allTitlesJoined() {
        var result = drills.allTitlesJoined();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals("The Silent Harbour | Mekong Mornings | Concrete and Rain | "
                + "Building Web APIs | Databases in Practice | The Patient Gardener | "
                + "Kitchen Notes | A History of Angkor | Night Market | Clean Java", result);
    }

    @Test
    @DisplayName("E3 · price statistics in one pass")
    void priceStatistics() {
        var stats = drills.priceStatistics();
        assertNotNull(stats, "still returning null — write the collector");
        assertEquals(10, stats.getCount());
        assertEquals(240.73, stats.getSum(), 0.0001);
        assertEquals(8.50, stats.getMin(), 0.0001);
        assertEquals(52.00, stats.getMax(), 0.0001);
        assertEquals(24.073, stats.getAverage(), 0.0001);
    }

    @Test
    @DisplayName("E4 · copies sold per author, clashes summed")
    void copiesSoldByAuthor() {
        Map<String, Integer> result = drills.copiesSoldByAuthor();
        assertNotNull(result, "still returning null — write the collector");
        assertEquals(5, result.size(), "five authors");
        assertEquals(10300, (int) result.get("Sokha Meas"), "4200 + 6100");
        assertEquals(9100, (int) result.get("Dara Chan"), "1800 + 7300");
        assertEquals(5000, (int) result.get("Vichea Ly"), "900 + 1500 + 2600");
        assertEquals(8700, (int) result.get("Bopha Sen"), "3300 + 5400");
        assertEquals(2100, (int) result.get("Rithy Pen"));
    }

    @Test
    @DisplayName("E5 · titles by decade, decades in order")
    void titlesByDecade() {
        Map<Integer, List<String>> result = drills.titlesByDecade();
        assertNotNull(result, "still returning null — write the collector");
        assertEquals(List.of(2010, 2020), List.copyOf(result.keySet()),
                "the decades must come out in order — did you choose the Map type?");
        assertEquals(List.of("The Silent Harbour", "Concrete and Rain",
                        "Databases in Practice", "A History of Angkor"),
                result.get(2010));
        assertEquals(List.of("Mekong Mornings", "Building Web APIs",
                        "The Patient Gardener", "Kitchen Notes",
                        "Night Market", "Clean Java"),
                result.get(2020));
    }

    @Test
    @DisplayName("E6 · authors with more than one book")
    void prolificAuthors() {
        var result = drills.prolificAuthors();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(List.of("Bopha Sen", "Dara Chan", "Sokha Meas", "Vichea Ly"), result);
    }
}
