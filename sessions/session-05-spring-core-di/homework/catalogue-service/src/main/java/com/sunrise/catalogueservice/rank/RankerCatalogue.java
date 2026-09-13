package com.sunrise.catalogueservice.rank;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RankerCatalogue {

    private final List<SearchRanker> rankers;

    public RankerCatalogue(List<SearchRanker> rankers) {
        this.rankers = List.copyOf(rankers);
    }

    public List<String> names() {
        return rankers.stream()
                .map(ranker -> ranker.getClass().getSimpleName())
                .toList();
    }
}
