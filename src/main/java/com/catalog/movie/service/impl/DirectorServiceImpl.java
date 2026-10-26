package com.catalog.movie.service.impl;

import com.catalog.movie.dto.DirectorRequest;
import com.catalog.movie.dto.DirectorResponse;
import com.catalog.movie.dto.MovieResponse;
import com.catalog.movie.dto.PageResponse;
import com.catalog.movie.entities.Director;
import com.catalog.movie.exception.ConflictException;
import com.catalog.movie.exception.ResourceNotFoundException;
import com.catalog.movie.mapper.CatalogMapper;
import com.catalog.movie.repository.DirectorRepository;
import com.catalog.movie.repository.MovieRepository;
import com.catalog.movie.service.DirectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectorServiceImpl implements DirectorService {

    private final DirectorRepository directorRepository;
    private final MovieRepository movieRepository;

    @Autowired
    public DirectorServiceImpl(DirectorRepository directorRepository, MovieRepository movieRepository) {
        this.directorRepository = directorRepository;
        this.movieRepository = movieRepository;
    }

    @Override
    @Transactional
    public DirectorResponse createDirector(DirectorRequest request) {
        Director director = new Director();
        apply(director, request);
        return CatalogMapper.toResponse(directorRepository.save(director));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DirectorResponse> getAllDirectors(Pageable pageable) {
        return PageResponse.from(directorRepository.findAll(pageable), CatalogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DirectorResponse getDirectorById(Long id) {
        return CatalogMapper.toResponse(findDirector(id));
    }

    @Override
    @Transactional
    public DirectorResponse updateDirector(Long id, DirectorRequest request) {
        Director director = findDirector(id);
        apply(director, request);
        return CatalogMapper.toResponse(directorRepository.save(director));
    }

    @Override
    @Transactional
    public void deleteDirector(Long id) {
        Director director = findDirector(id);
        if (movieRepository.existsByDirectorId(id)) {
            throw new ConflictException(
                    "Director " + id + " still has movies; delete or reassign them before deleting the director");
        }
        directorRepository.delete(director);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> getMoviesByDirector(Long id, Pageable pageable) {
        if (!directorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Director", id);
        }
        return PageResponse.from(movieRepository.findByDirectorId(id, pageable), CatalogMapper::toResponse);
    }

    private Director findDirector(Long id) {
        return directorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Director", id));
    }

    private static void apply(Director director, DirectorRequest request) {
        director.setName(request.name().trim());
        director.setNationality(request.nationality());
        director.setBirthYear(request.birthYear());
    }
}
