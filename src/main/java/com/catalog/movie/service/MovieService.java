package com.catalog.movie.service;

import com.catalog.movie.dto.MovieRequest;
import com.catalog.movie.dto.MovieResponse;
import com.catalog.movie.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface MovieService {
    MovieResponse createMovie(MovieRequest request);

    PageResponse<MovieResponse> getMovies(String genre, Integer year, Pageable pageable);

    MovieResponse getMovieById(Long id);

    MovieResponse updateMovie(Long id, MovieRequest request);

    void deleteMovie(Long id);

    MovieResponse addGenre(Long movieId, Long genreId);

    MovieResponse removeGenre(Long movieId, Long genreId);
}
