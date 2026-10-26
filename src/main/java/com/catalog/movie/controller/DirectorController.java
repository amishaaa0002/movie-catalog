package com.catalog.movie.controller;

import com.catalog.movie.dto.DirectorRequest;
import com.catalog.movie.dto.DirectorResponse;
import com.catalog.movie.dto.MovieResponse;
import com.catalog.movie.dto.PageResponse;
import com.catalog.movie.service.DirectorService;
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

import static com.catalog.movie.utils.ApiMappings.DIRECTORS;

@Tag(name = "Directors")
@RestController
@RequestMapping(DIRECTORS)
public class DirectorController {

    private final DirectorService directorService;

    @Autowired
    public DirectorController(DirectorService directorService) {
        this.directorService = directorService;
    }

    @Operation(summary = "Create a new director")
    @PostMapping
    public ResponseEntity<DirectorResponse> createDirector(@Valid @RequestBody DirectorRequest request) {
        DirectorResponse created = directorService.createDirector(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List all directors")
    @GetMapping
    public PageResponse<DirectorResponse> getAllDirectors(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return directorService.getAllDirectors(SortValidator.validate(pageable, SortValidator.DIRECTOR_SORT));
    }

    @Operation(summary = "Get director by ID")
    @GetMapping("/{id}")
    public DirectorResponse getDirectorById(@PathVariable Long id) {
        return directorService.getDirectorById(id);
    }

    @Operation(summary = "Update a director")
    @PutMapping("/{id}")
    public DirectorResponse updateDirector(@PathVariable Long id, @Valid @RequestBody DirectorRequest request) {
        return directorService.updateDirector(id, request);
    }

    @Operation(summary = "Delete a director")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDirector(@PathVariable Long id) {
        directorService.deleteDirector(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all movies by a director")
    @GetMapping("/{id}/movies")
    public PageResponse<MovieResponse> getMoviesByDirector(
            @PathVariable Long id,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return directorService.getMoviesByDirector(id, SortValidator.validate(pageable, SortValidator.MOVIE_SORT));
    }
}
