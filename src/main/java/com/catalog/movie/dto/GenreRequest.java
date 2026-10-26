package com.catalog.movie.dto;

import jakarta.validation.constraints.NotBlank;

public record GenreRequest(
        @NotBlank(message = "name is required")
        String name
) {
}
