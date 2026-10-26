package com.catalog.movie.service.impl;

import com.catalog.movie.config.CatalogProperties;
import com.catalog.movie.dto.MovieRequest;
import com.catalog.movie.dto.MovieResponse;
import com.catalog.movie.dto.PageResponse;
import com.catalog.movie.entities.Director;
import com.catalog.movie.entities.Genre;
import com.catalog.movie.entities.Movie;
import com.catalog.movie.exception.BusinessRuleException;
import com.catalog.movie.exception.ResourceNotFoundException;
import com.catalog.movie.mapper.CatalogMapper;
import com.catalog.movie.repository.DirectorRepository;
import com.catalog.movie.repository.GenreRepository;
import com.catalog.movie.repository.MovieRepository;
import com.catalog.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final DirectorRepository directorRepository;
    private final GenreRepository genreRepository;
    private final CatalogProperties catalogProperties;

    @Autowired
    public MovieServiceImpl(MovieRepository movieRepository, DirectorRepository directorRepository,
                            GenreRepository genreRepository, CatalogProperties catalogProperties) {
        this.movieRepository = movieRepository;
        this.directorRepository = directorRepository;
        this.genreRepository = genreRepository;
        this.catalogProperties = catalogProperties;
    }

    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        Movie movie = new Movie();
        apply(movie, request);
        movie.setGenres(resolveGenres(request.genreIds()));
        return CatalogMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> getMovies(String genre, Integer year, Pageable pageable) {
        String genreFilter = StringUtils.hasText(genre) ? genre.trim() : null;
        return PageResponse.from(movieRepository.search(genreFilter, year, pageable), CatalogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        return CatalogMapper.toResponse(findMovie(id));
    }

    /**
     * Full update replaces the movie's genres when present
     * when omitted (null) the existing genres are kept.
     */
    @Override
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie movie = findMovie(id);
        apply(movie, request);
        if (request.genreIds() != null) {
            movie.setGenres(resolveGenres(request.genreIds()));
        }
        return CatalogMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public void deleteMovie(Long id) {
        movieRepository.delete(findMovie(id));
    }

    /** Idempotent: associating a genre the movie already has is a no-op. */
    @Override
    @Transactional
    public MovieResponse addGenre(Long movieId, Long genreId) {
        Movie movie = findMovie(movieId);
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResourceNotFoundException("Genre", genreId));

        if (hasGenre(movie, genreId)) {
            return CatalogMapper.toResponse(movie);
        }
        if (movie.getGenres().size() >= catalogProperties.maxGenresPerMovie()) {
            throw new BusinessRuleException(
                    "Movie " + movieId + " already has the maximum of "
                            + catalogProperties.maxGenresPerMovie() + " genres");
        }
        movie.getGenres().add(genre);
        return CatalogMapper.toResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public MovieResponse removeGenre(Long movieId, Long genreId) {
        Movie movie = findMovie(movieId);
        if (!genreRepository.existsById(genreId)) {
            throw new ResourceNotFoundException("Genre", genreId);
        }
        boolean removed = movie.getGenres().removeIf(g -> g.getId().equals(genreId));
        if (!removed) {
            throw new ResourceNotFoundException(
                    "Genre " + genreId + " is not associated with movie " + movieId);
        }
        return CatalogMapper.toResponse(movieRepository.save(movie));
    }

    private Movie findMovie(Long id) {
        return movieRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    private void apply(Movie movie, MovieRequest request) {
        movie.setTitle(request.title().trim());
        movie.setReleaseYear(request.releaseYear());
        movie.setRating(request.rating() != null ? request.rating() : catalogProperties.defaultRating());
        movie.setDirector(findDirector(request.directorId()));
    }

    private Director findDirector(Long id) {
        return directorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Director", id));
    }

    /** Validates the genre count against config and that every id exists. */
    private Set<Genre> resolveGenres(Set<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return new HashSet<>();
        }
        int max = catalogProperties.maxGenresPerMovie();
        if (genreIds.size() > max) {
            throw new BusinessRuleException(
                    "A movie can have at most " + max + " genres, but " + genreIds.size() + " were supplied");
        }
        List<Genre> found = genreRepository.findAllById(genreIds);
        if (found.size() != genreIds.size()) {
            Set<Long> foundIds = found.stream().map(Genre::getId).collect(Collectors.toSet());
            List<Long> missing = genreIds.stream().filter(id -> !foundIds.contains(id)).sorted().toList();
            throw new ResourceNotFoundException("Genre(s) not found with id(s) " + missing);
        }
        return new HashSet<>(found);
    }

    private static boolean hasGenre(Movie movie, Long genreId) {
        return movie.getGenres().stream().anyMatch(g -> g.getId().equals(genreId));
    }
}
