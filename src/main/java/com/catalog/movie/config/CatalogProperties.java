package com.catalog.movie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catalog")
public record CatalogProperties(int maxGenresPerMovie, double defaultRating) {
}
