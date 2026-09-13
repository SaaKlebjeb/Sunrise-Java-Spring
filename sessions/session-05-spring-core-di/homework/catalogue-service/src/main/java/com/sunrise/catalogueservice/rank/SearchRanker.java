package com.sunrise.catalogueservice.rank;

import com.sunrise.catalogueservice.domain.CatalogueEntry;

import java.util.List;

public interface SearchRanker {

    List<CatalogueEntry> rank(List<CatalogueEntry> entries);
}
