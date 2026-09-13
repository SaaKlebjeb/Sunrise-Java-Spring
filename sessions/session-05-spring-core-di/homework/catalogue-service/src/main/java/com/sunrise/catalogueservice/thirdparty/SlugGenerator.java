package com.sunrise.catalogueservice.thirdparty;

import java.util.Locale;

/**
 * Pretend this class arrived in a jar: it carries no annotation, and it may not be given one.
 * The only way it becomes a bean is the @Bean method in CatalogueConfig.
 */
public class SlugGenerator {

    private final String separator;

    public SlugGenerator(String separator) {
        this.separator = separator;
    }

    public String slugify(String title) {
        String slug = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", separator);
        if (slug.startsWith(separator)) {
            slug = slug.substring(separator.length());
        }
        if (slug.endsWith(separator)) {
            slug = slug.substring(0, slug.length() - separator.length());
        }
        return slug;
    }
}
