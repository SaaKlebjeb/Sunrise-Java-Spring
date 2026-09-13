package com.sunrise.catalogueservice.repository;

import com.sunrise.catalogueservice.domain.CatalogueEntry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class InMemoryCatalogueRepository implements CatalogueRepository {

    private final List<CatalogueEntry> entries = new ArrayList<>();

    @PostConstruct
    void seed() {
        entries.add(new CatalogueEntry("978-0132350884", "Clean Code", "Robert Martin", 41));
        entries.add(new CatalogueEntry("978-0134685991", "Effective Java", "Joshua Bloch", 27));
        entries.add(new CatalogueEntry("978-0201616224", "The Pragmatic Programmer", "Andrew Hunt", 63));
        entries.add(new CatalogueEntry("978-0596009205", "Head First Design Patterns", "Eric Freeman", 12));
        System.out.println("seeded " + entries.size() + " entries");
    }

    @Override
    public List<CatalogueEntry> findAll() {
        return List.copyOf(entries);
    }

    @Override
    public List<CatalogueEntry> findByTitleContaining(String fragment) {
        String needle = fragment.toLowerCase(Locale.ROOT);
        return entries.stream()
                .filter(entry -> entry.title().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }
}
