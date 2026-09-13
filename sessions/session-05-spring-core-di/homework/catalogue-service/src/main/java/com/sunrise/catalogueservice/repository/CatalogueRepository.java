package com.sunrise.catalogueservice.repository;

import com.sunrise.catalogueservice.domain.CatalogueEntry;

import java.util.List;

public interface CatalogueRepository {

    List<CatalogueEntry> findAll();

    List<CatalogueEntry> findByTitleContaining(String fragment);
}
