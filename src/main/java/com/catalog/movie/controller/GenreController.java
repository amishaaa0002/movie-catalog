package com.catalog.movie.controller;

import com.catalog.movie.dto.GenreRequest;
import com.catalog.movie.dto.GenreResponse;
import com.catalog.movie.dto.PageResponse;
import com.catalog.movie.service.GenreService;
import com.catalog.movie.utils.SortValidator;
import io.swagger.v3.oas.annotations.Operation;
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

import static com.catalog.movie.utils.ApiMappings.GENRES;

@Tag(name = "Genres")
@RestController
@RequestMapping(GENRES)
public class GenreController {

    private final GenreService genreService;

    @Autowired
    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @Operation(summary = "Create a genre")
    @PostMapping
    public ResponseEntity<GenreResponse> createGenre(@Valid @RequestBody GenreRequest request) {
        GenreResponse created = genreService.createGenre(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List all genres")
    @GetMapping
    public PageResponse<GenreResponse> getAllGenres(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return genreService.getAllGenres(SortValidator.validate(pageable, SortValidator.GENRE_SORT));
    }

    @Operation(summary = "Get genre by ID")
    @GetMapping("/{id}")
    public GenreResponse getGenreById(@PathVariable Long id) {
        return genreService.getGenreById(id);
    }
}
