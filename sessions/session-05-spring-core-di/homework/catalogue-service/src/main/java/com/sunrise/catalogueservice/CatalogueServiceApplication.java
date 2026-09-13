package com.sunrise.catalogueservice;

import com.sunrise.catalogueservice.domain.CatalogueEntry;
import com.sunrise.catalogueservice.rank.RankerCatalogue;
import com.sunrise.catalogueservice.rank.SearchRanker;
import com.sunrise.catalogueservice.repository.CatalogueRepository;
import com.sunrise.catalogueservice.search.SearchService;
import com.sunrise.catalogueservice.thirdparty.SlugGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CatalogueServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogueServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(CatalogueRepository catalogue,
                           @Qualifier("alphabeticalRanker") SearchRanker alphabetical,
                           RankerCatalogue rankers,
                           SlugGenerator slugs,
                           SearchService search) {
        return args -> {
            System.out.println("catalogue service up");

            System.out.println("=== all ===");
            catalogue.findAll().forEach(entry -> System.out.println(entry.title()));

            System.out.println("=== alphabetical ===");
            alphabetical.rank(catalogue.findAll())
                    .forEach(entry -> System.out.println(entry.title()));

            System.out.println("=== rankers ===");
            rankers.names().forEach(System.out::println);

            System.out.println("=== slugs ===");
            for (CatalogueEntry entry : catalogue.findAll()) {
                System.out.println(slugs.slugify(entry.title()));
            }

            System.out.println("=== search: \"a\" ===");
            search.search("a").forEach(System.out::println);
        };
    }
}
