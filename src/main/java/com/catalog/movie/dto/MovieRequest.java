package com.catalog.movie.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record MovieRequest(
        @NotBlank(message = "title is required")
        String title,

        @NotNull(message = "releaseYear is required")
        Integer releaseYear,

        @DecimalMin(value = "0.0", message = "rating must be between 0.0 and 10.0")
        @DecimalMax(value = "10.0", message = "rating must be between 0.0 and 10.0")
        Double rating,

        @NotNull(message = "directorId is required")
        Long directorId,

        Set<Long> genreIds
) {
}
