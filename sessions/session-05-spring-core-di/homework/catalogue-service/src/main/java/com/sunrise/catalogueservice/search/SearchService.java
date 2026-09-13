package com.sunrise.catalogueservice.search;

import com.sunrise.catalogueservice.domain.CatalogueEntry;
import com.sunrise.catalogueservice.rank.SearchRanker;
import com.sunrise.catalogueservice.repository.CatalogueRepository;
import com.sunrise.catalogueservice.thirdparty.SlugGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final CatalogueRepository catalogue;
    private final SearchRanker ranker;
    private final SlugGenerator slugs;

    public SearchService(CatalogueRepository catalogue,
                         @Qualifier("popularityRanker") SearchRanker ranker,
                         SlugGenerator slugs) {
        this.catalogue = catalogue;
        this.ranker = ranker;
        this.slugs = slugs;
    }

    public List<String> search(String fragment) {
        List<CatalogueEntry> matches = catalogue.findByTitleContaining(fragment);
        return ranker.rank(matches).stream()
                .map(entry -> slugs.slugify(entry.title())
                        + " (borrowed " + entry.timesBorrowed() + " times)")
                .toList();
    }
}
