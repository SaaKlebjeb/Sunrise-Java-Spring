package com.sunrise.catalogueservice.rank;

import com.sunrise.catalogueservice.domain.CatalogueEntry;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@Order(2)
public class AlphabeticalRanker implements SearchRanker {

    @Override
    public List<CatalogueEntry> rank(List<CatalogueEntry> entries) {
        return entries.stream()
                .sorted(Comparator.comparing(CatalogueEntry::title))
                .toList();
    }
}
