package com.catalog.movie.dto;

import java.util.List;

public record MovieResponse(
        Long id,
        String title,
        Integer releaseYear,
        Double rating,
        DirectorSummary director,
        List<GenreResponse> genres
) {
    public record DirectorSummary(Long id, String name) {
    }
}
