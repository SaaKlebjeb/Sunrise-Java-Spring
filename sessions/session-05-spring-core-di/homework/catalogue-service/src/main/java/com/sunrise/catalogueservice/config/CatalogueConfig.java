package com.sunrise.catalogueservice.config;

import com.sunrise.catalogueservice.thirdparty.SlugGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogueConfig {

    @Bean
    public SlugGenerator slugGenerator() {
        return new SlugGenerator("-");
    }
}
