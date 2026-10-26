package com.catalog.movie.service;

import com.catalog.movie.dto.GenreRequest;
import com.catalog.movie.dto.GenreResponse;
import com.catalog.movie.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface GenreService {
    GenreResponse createGenre(GenreRequest request);

    PageResponse<GenreResponse> getAllGenres(Pageable pageable);

    GenreResponse getGenreById(Long id);
}
