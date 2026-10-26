package com.catalog.movie.mapper;

import com.catalog.movie.dto.DirectorResponse;
import com.catalog.movie.dto.GenreResponse;
import com.catalog.movie.dto.MovieResponse;
import com.catalog.movie.entities.Director;
import com.catalog.movie.entities.Genre;
import com.catalog.movie.entities.Movie;

import java.util.Comparator;

/**
 * Converts entities to API responses. Must be called inside a transaction for lazy associations.
 */
public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static DirectorResponse toResponse(Director director) {
        return new DirectorResponse(
                director.getId(),
                director.getName(),
                director.getNationality(),
                director.getBirthYear());
    }

    public static GenreResponse toResponse(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getName());
    }

    public static MovieResponse toResponse(Movie movie) {
        Director director = movie.getDirector();
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getReleaseYear(),
                movie.getRating(),
                new MovieResponse.DirectorSummary(director.getId(), director.getName()),
                movie.getGenres().stream()
                        .sorted(Comparator.comparing(Genre::getName))
                        .map(CatalogMapper::toResponse)
                        .toList());
    }
}
