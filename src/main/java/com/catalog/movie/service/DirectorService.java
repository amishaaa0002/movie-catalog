package com.catalog.movie.service;

import com.catalog.movie.dto.DirectorRequest;
import com.catalog.movie.dto.DirectorResponse;
import com.catalog.movie.dto.MovieResponse;
import com.catalog.movie.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface DirectorService {
    DirectorResponse createDirector(DirectorRequest request);

    PageResponse<DirectorResponse> getAllDirectors(Pageable pageable);

    DirectorResponse getDirectorById(Long id);

    DirectorResponse updateDirector(Long id, DirectorRequest request);

    void deleteDirector(Long id);

    PageResponse<MovieResponse> getMoviesByDirector(Long id, Pageable pageable);
}
