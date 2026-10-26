package com.catalog.movie.service;

import com.catalog.movie.dto.DirectorRequest;
import com.catalog.movie.dto.DirectorResponse;
import com.catalog.movie.dto.MovieResponse;
import com.catalog.movie.dto.PageResponse;
import com.catalog.movie.entities.Director;
import com.catalog.movie.entities.Movie;
import com.catalog.movie.exception.ConflictException;
import com.catalog.movie.exception.ResourceNotFoundException;
import com.catalog.movie.repository.DirectorRepository;
import com.catalog.movie.repository.MovieRepository;
import com.catalog.movie.service.impl.DirectorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectorServiceImplTest {

    @Mock
    private DirectorRepository directorRepository;
    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private DirectorServiceImpl directorService;

    @Test
    void createDirectorSavesTrimmedFields() {
        when(directorRepository.save(any(Director.class))).thenAnswer(invocation -> {
            Director d = invocation.getArgument(0);
            d.setId(1L);
            return d;
        });

        DirectorResponse response = directorService.createDirector(
                new DirectorRequest("  Christopher Nolan ", "British", 1970));

        assertThat(response).isEqualTo(new DirectorResponse(1L, "Christopher Nolan", "British", 1970));
    }

    @Test
    void getAllDirectorsReturnsPage() {
        Pageable pageable = PageRequest.of(0, 2);
        when(directorRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(director(1L), director(2L)), pageable, 3));

        PageResponse<DirectorResponse> page = directorService.getAllDirectors(pageable);

        assertThat(page.content()).extracting(DirectorResponse::id).containsExactly(1L, 2L);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void getDirectorByIdThrowsWhenMissing() {
        when(directorRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> directorService.getDirectorById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Director not found with id 9");
    }

    @Test
    void updateDirectorOverwritesAllFields() {
        Director existing = director(1L);
        when(directorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(directorRepository.save(any(Director.class))).thenAnswer(returnsFirstArg());

        directorService.updateDirector(1L, new DirectorRequest("C. Nolan", "British-American", 1971));

        ArgumentCaptor<Director> saved = ArgumentCaptor.forClass(Director.class);
        verify(directorRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("C. Nolan");
        assertThat(saved.getValue().getNationality()).isEqualTo("British-American");
        assertThat(saved.getValue().getBirthYear()).isEqualTo(1971);
    }

    @Test
    void updateDirectorThrowsWhenMissing() {
        when(directorRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> directorService.updateDirector(9L, new DirectorRequest("X", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(directorRepository, never()).save(any());
    }

    @Test
    void deleteDirectorWithoutMoviesDeletes() {
        Director existing = director(1L);
        when(directorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(movieRepository.existsByDirectorId(1L)).thenReturn(false);

        directorService.deleteDirector(1L);

        verify(directorRepository).delete(existing);
    }

    @Test
    void deleteDirectorWithMoviesIsRejected() {
        when(directorRepository.findById(1L)).thenReturn(Optional.of(director(1L)));
        when(movieRepository.existsByDirectorId(1L)).thenReturn(true);

        assertThatThrownBy(() -> directorService.deleteDirector(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("still has movies");
        verify(directorRepository, never()).delete(any());
    }

    @Test
    void deleteDirectorThrowsWhenMissing() {
        when(directorRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> directorService.deleteDirector(9L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(directorRepository, never()).delete(any());
    }

    @Test
    void getMoviesByDirectorReturnsDirectorsMovies() {
        Pageable pageable = PageRequest.of(0, 20);
        Movie movie = new Movie();
        movie.setId(5L);
        movie.setTitle("Inception");
        movie.setDirector(director(1L));
        when(directorRepository.existsById(1L)).thenReturn(true);
        when(movieRepository.findByDirectorId(1L, pageable)).thenReturn(new PageImpl<>(List.of(movie), pageable, 1));

        PageResponse<MovieResponse> page = directorService.getMoviesByDirector(1L, pageable);

        assertThat(page.content()).singleElement()
                .satisfies(m -> {
                    assertThat(m.title()).isEqualTo("Inception");
                    assertThat(m.director().id()).isEqualTo(1L);
                });
    }

    @Test
    void getMoviesByDirectorThrowsWhenDirectorMissing() {
        when(directorRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> directorService.getMoviesByDirector(9L, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Director not found with id 9");
        verify(movieRepository, never()).findByDirectorId(any(), any());
    }

    private static Director director(Long id) {
        Director director = new Director();
        director.setId(id);
        director.setName("Director " + id);
        return director;
    }
}
