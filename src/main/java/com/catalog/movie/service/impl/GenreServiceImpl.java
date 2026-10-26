package com.catalog.movie.service.impl;

import com.catalog.movie.dto.GenreRequest;
import com.catalog.movie.dto.GenreResponse;
import com.catalog.movie.dto.PageResponse;
import com.catalog.movie.entities.Genre;
import com.catalog.movie.exception.ConflictException;
import com.catalog.movie.exception.ResourceNotFoundException;
import com.catalog.movie.mapper.CatalogMapper;
import com.catalog.movie.repository.GenreRepository;
import com.catalog.movie.service.GenreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    @Autowired
    public GenreServiceImpl(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Override
    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        String name = request.name().trim();
        if (genreRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Genre '" + name + "' already exists");
        }
        Genre genre = new Genre();
        genre.setName(name);
        return CatalogMapper.toResponse(genreRepository.save(genre));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GenreResponse> getAllGenres(Pageable pageable) {
        return PageResponse.from(genreRepository.findAll(pageable), CatalogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public GenreResponse getGenreById(Long id) {
        return genreRepository.findById(id)
                .map(CatalogMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Genre", id));
    }
}
