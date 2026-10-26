package com.catalog.movie.service;

import com.catalog.movie.dto.GenreRequest;
import com.catalog.movie.dto.GenreResponse;
import com.catalog.movie.dto.PageResponse;
import com.catalog.movie.entities.Genre;
import com.catalog.movie.exception.ConflictException;
import com.catalog.movie.exception.ResourceNotFoundException;
import com.catalog.movie.repository.GenreRepository;
import com.catalog.movie.service.impl.GenreServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreServiceImpl genreService;

    @Test
    void createGenreSavesTrimmedName() {
        when(genreRepository.existsByNameIgnoreCase("Sci-Fi")).thenReturn(false);
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> {
            Genre g = invocation.getArgument(0);
            g.setId(1L);
            return g;
        });

        GenreResponse response = genreService.createGenre(new GenreRequest(" Sci-Fi "));

        assertThat(response).isEqualTo(new GenreResponse(1L, "Sci-Fi"));
    }

    @Test
    void createGenreRejectsDuplicateNameIgnoringCase() {
        when(genreRepository.existsByNameIgnoreCase("drama")).thenReturn(true);

        assertThatThrownBy(() -> genreService.createGenre(new GenreRequest("drama")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Genre 'drama' already exists");
        verify(genreRepository, never()).save(any());
    }

    @Test
    void getAllGenresReturnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Genre action = new Genre();
        action.setId(1L);
        action.setName("Action");
        when(genreRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(action), pageable, 1));

        PageResponse<GenreResponse> page = genreService.getAllGenres(pageable);

        assertThat(page.content()).containsExactly(new GenreResponse(1L, "Action"));
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void getGenreByIdThrowsWhenMissing() {
        when(genreRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.getGenreById(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Genre not found with id 7");
    }
}
