package com.catalog.movie.dto;

import jakarta.validation.constraints.NotBlank;

public record DirectorRequest(
        @NotBlank(message = "name is required")
        String name,
        String nationality,
        Integer birthYear
) {
}
