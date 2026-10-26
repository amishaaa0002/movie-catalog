package com.catalog.movie.controller;

import com.catalog.movie.dto.MovieRequest;
import com.catalog.movie.dto.MovieResponse;
import com.catalog.movie.dto.PageResponse;
import com.catalog.movie.service.MovieService;
import com.catalog.movie.utils.SortValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static com.catalog.movie.utils.ApiMappings.MOVIES;

@Tag(name = "Movies")
@RestController
@RequestMapping(MOVIES)
public class MovieController {

    private final MovieService movieService;

    @Autowired
    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @Operation(summary = "Create a movie with default ratings")
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody MovieRequest request) {
        MovieResponse created = movieService.createMovie(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List movies, optionally filtered by genre name and/or release year")
    @GetMapping
    public PageResponse<MovieResponse> getMovies(
            @Parameter(description = "Genre name, case-insensitive", example = "Drama")
            @RequestParam(required = false) String genre,
            @Parameter(description = "Release year", example = "2010")
            @RequestParam(required = false) Integer year,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return movieService.getMovies(genre, year, SortValidator.validate(pageable, SortValidator.MOVIE_SORT));
    }

    @Operation(summary = "Get movie by ID with its director and genres")
    @GetMapping("/{id}")
    public MovieResponse getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    @Operation(summary = "Update movie details")
    @PutMapping("/{id}")
    public MovieResponse updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return movieService.updateMovie(id, request);
    }

    @Operation(summary = "Delete a movie")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Associate a genre with a movie (idempotent)")
    @PostMapping("/{id}/genres/{gid}")
    public MovieResponse addGenre(@PathVariable Long id, @PathVariable Long gid) {
        return movieService.addGenre(id, gid);
    }

    @Operation(summary = "Remove a genre from a movie")
    @DeleteMapping("/{id}/genres/{gid}")
    public MovieResponse removeGenre(@PathVariable Long id, @PathVariable Long gid) {
        return movieService.removeGenre(id, gid);
    }
}
