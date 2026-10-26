package com.catalog.movie.service;

import com.catalog.movie.config.CatalogProperties;
import com.catalog.movie.dto.MovieRequest;
import com.catalog.movie.dto.MovieResponse;
import com.catalog.movie.dto.PageResponse;
import com.catalog.movie.entities.Director;
import com.catalog.movie.entities.Genre;
import com.catalog.movie.entities.Movie;
import com.catalog.movie.exception.BusinessRuleException;
import com.catalog.movie.exception.ResourceNotFoundException;
import com.catalog.movie.repository.DirectorRepository;
import com.catalog.movie.repository.GenreRepository;
import com.catalog.movie.repository.MovieRepository;
import com.catalog.movie.service.impl.MovieServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    private static final int MAX_GENRES = 3;
    private static final double DEFAULT_RATING = 7.0;

    @Mock
    private MovieRepository movieRepository;
    @Mock
    private DirectorRepository directorRepository;
    @Mock
    private GenreRepository genreRepository;
    @Spy
    private CatalogProperties catalogProperties = new CatalogProperties(MAX_GENRES, DEFAULT_RATING);

    @InjectMocks
    private MovieServiceImpl movieService;

    private Director nolan;
    private Genre sciFi;
    private Genre drama;

    @BeforeEach
    void setUp() {
        nolan = director(1L, "Christopher Nolan");
        sciFi = genre(10L, "Sci-Fi");
        drama = genre(20L, "Drama");
    }

    @Nested
    class CreateMovie {

        @Test
        void appliesDefaultRatingFromConfigWhenRatingMissing() {
            when(directorRepository.findById(1L)).thenReturn(Optional.of(nolan));
            when(movieRepository.save(any(Movie.class))).thenAnswer(returnsFirstArg());

            MovieResponse response = movieService.createMovie(request(null, null));

            assertThat(response.rating()).isEqualTo(DEFAULT_RATING);
            assertThat(response.director().name()).isEqualTo("Christopher Nolan");
            assertThat(response.genres()).isEmpty();
            verifyNoInteractions(genreRepository);
        }

        @Test
        void keepsSuppliedRatingAndResolvesGenres() {
            when(directorRepository.findById(1L)).thenReturn(Optional.of(nolan));
            when(genreRepository.findAllById(Set.of(10L, 20L))).thenReturn(List.of(sciFi, drama));
            when(movieRepository.save(any(Movie.class))).thenAnswer(returnsFirstArg());

            movieService.createMovie(request(8.8, Set.of(10L, 20L)));

            ArgumentCaptor<Movie> saved = ArgumentCaptor.forClass(Movie.class);
            verify(movieRepository).save(saved.capture());
            Movie movie = saved.getValue();
            assertThat(movie.getTitle()).isEqualTo("Inception");
            assertThat(movie.getReleaseYear()).isEqualTo(2010);
            assertThat(movie.getRating()).isEqualTo(8.8);
            assertThat(movie.getDirector()).isSameAs(nolan);
            assertThat(movie.getGenres()).containsExactlyInAnyOrder(sciFi, drama);
        }

        @Test
        void rejectsMoreGenresThanConfiguredMaximum() {
            when(directorRepository.findById(1L)).thenReturn(Optional.of(nolan));

            assertThatThrownBy(() -> movieService.createMovie(request(null, Set.of(1L, 2L, 3L, 4L))))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A movie can have at most 3 genres, but 4 were supplied");

            verify(genreRepository, never()).findAllById(any());
            verify(movieRepository, never()).save(any());
        }

        @Test
        void failsWhenAGenreDoesNotExist() {
            when(directorRepository.findById(1L)).thenReturn(Optional.of(nolan));
            when(genreRepository.findAllById(Set.of(10L, 99L))).thenReturn(List.of(sciFi));

            assertThatThrownBy(() -> movieService.createMovie(request(null, Set.of(10L, 99L))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Genre(s) not found with id(s) [99]");

            verify(movieRepository, never()).save(any());
        }

        @Test
        void failsWhenDirectorDoesNotExist() {
            when(directorRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> movieService.createMovie(request(null, null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Director not found with id 1");

            verify(movieRepository, never()).save(any());
        }
    }

    @Nested
    class ReadMovies {

        @Test
        void getMovieByIdThrowsWhenMissing() {
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> movieService.getMovieById(5L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Movie not found with id 5");
        }

        @Test
        void getMovieByIdMapsDirectorAndSortedGenres() {
            Movie movie = movie(5L, sciFi, drama);
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(movie));

            MovieResponse response = movieService.getMovieById(5L);

            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.director().id()).isEqualTo(1L);
            assertThat(response.genres()).extracting("name").containsExactly("Drama", "Sci-Fi");
        }

        @Test
        void getMoviesTreatsBlankGenreAsNoFilter() {
            Pageable pageable = PageRequest.of(0, 20);
            when(movieRepository.search(null, 2010, pageable))
                    .thenReturn(new PageImpl<>(List.of(movie(5L, sciFi)), pageable, 1));

            PageResponse<MovieResponse> page = movieService.getMovies("  ", 2010, pageable);

            assertThat(page.totalElements()).isEqualTo(1);
            assertThat(page.content()).extracting(MovieResponse::id).containsExactly(5L);
        }

        @Test
        void getMoviesTrimsGenreFilter() {
            Pageable pageable = PageRequest.of(0, 20);
            when(movieRepository.search("Drama", null, pageable)).thenReturn(Page.empty(pageable));

            PageResponse<MovieResponse> page = movieService.getMovies(" Drama ", null, pageable);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalPages()).isZero();
        }
    }

    @Nested
    class UpdateMovie {

        @Test
        void keepsExistingGenresWhenGenreIdsOmitted() {
            Movie existing = movie(5L, sciFi);
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(existing));
            when(directorRepository.findById(1L)).thenReturn(Optional.of(nolan));
            when(movieRepository.save(existing)).thenReturn(existing);

            MovieResponse response = movieService.updateMovie(5L,
                    new MovieRequest("Inception (Remastered)", 2011, 9.0, 1L, null));

            assertThat(response.title()).isEqualTo("Inception (Remastered)");
            assertThat(response.releaseYear()).isEqualTo(2011);
            assertThat(response.rating()).isEqualTo(9.0);
            assertThat(response.genres()).extracting("name").containsExactly("Sci-Fi");
            verifyNoInteractions(genreRepository);
        }

        @Test
        void replacesGenresWhenGenreIdsSupplied() {
            Movie existing = movie(5L, sciFi);
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(existing));
            when(directorRepository.findById(1L)).thenReturn(Optional.of(nolan));
            when(genreRepository.findAllById(Set.of(20L))).thenReturn(List.of(drama));
            when(movieRepository.save(existing)).thenReturn(existing);

            MovieResponse response = movieService.updateMovie(5L, request(null, Set.of(20L)));

            assertThat(response.genres()).extracting("name").containsExactly("Drama");
            assertThat(response.rating()).isEqualTo(DEFAULT_RATING);
        }

        @Test
        void throwsWhenMovieMissing() {
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> movieService.updateMovie(5L, request(null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(movieRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteMovie {

        @Test
        void deletesExistingMovie() {
            Movie existing = movie(5L);
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(existing));

            movieService.deleteMovie(5L);

            verify(movieRepository).delete(existing);
        }

        @Test
        void throwsWhenMovieMissing() {
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> movieService.deleteMovie(5L))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(movieRepository, never()).delete(any());
        }
    }

    @Nested
    class GenreAssociation {

        @Test
        void addsGenreToMovie() {
            Movie existing = movie(5L, sciFi);
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(existing));
            when(genreRepository.findById(20L)).thenReturn(Optional.of(drama));
            when(movieRepository.save(existing)).thenReturn(existing);

            MovieResponse response = movieService.addGenre(5L, 20L);

            assertThat(response.genres()).extracting("name").containsExactly("Drama", "Sci-Fi");
        }

        @Test
        void addingAnAlreadyAssociatedGenreIsANoOp() {
            Movie existing = movie(5L, sciFi);
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(existing));
            when(genreRepository.findById(10L)).thenReturn(Optional.of(sciFi));

            MovieResponse response = movieService.addGenre(5L, 10L);

            assertThat(response.genres()).hasSize(1);
            verify(movieRepository, never()).save(any());
        }

        @Test
        void rejectsAddingBeyondConfiguredMaximum() {
            Movie existing = movie(5L, genre(1L, "A"), genre(2L, "B"), genre(3L, "C"));
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(existing));
            when(genreRepository.findById(20L)).thenReturn(Optional.of(drama));

            assertThatThrownBy(() -> movieService.addGenre(5L, 20L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Movie 5 already has the maximum of 3 genres");

            assertThat(existing.getGenres()).hasSize(3);
            verify(movieRepository, never()).save(any());
        }

        @Test
        void addThrowsWhenGenreMissing() {
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(movie(5L)));
            when(genreRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> movieService.addGenre(5L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Genre not found with id 99");
        }

        @Test
        void removesAssociatedGenre() {
            Movie existing = movie(5L, sciFi, drama);
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(existing));
            when(genreRepository.existsById(20L)).thenReturn(true);
            when(movieRepository.save(existing)).thenReturn(existing);

            MovieResponse response = movieService.removeGenre(5L, 20L);

            assertThat(response.genres()).extracting("name").containsExactly("Sci-Fi");
        }

        @Test
        void removeThrowsWhenGenreNotAssociated() {
            when(movieRepository.findWithDetailsById(5L)).thenReturn(Optional.of(movie(5L, sciFi)));
            when(genreRepository.existsById(20L)).thenReturn(true);

            assertThatThrownBy(() -> movieService.removeGenre(5L, 20L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Genre 20 is not associated with movie 5");
            verify(movieRepository, never()).save(any());
        }
    }

    private static MovieRequest request(Double rating, Set<Long> genreIds) {
        return new MovieRequest("Inception", 2010, rating, 1L, genreIds);
    }

    private Movie movie(Long id, Genre... genres) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle("Inception");
        movie.setReleaseYear(2010);
        movie.setRating(8.8);
        movie.setDirector(nolan);
        movie.setGenres(new HashSet<>(List.of(genres)));
        return movie;
    }

    private static Director director(Long id, String name) {
        Director director = new Director();
        director.setId(id);
        director.setName(name);
        return director;
    }

    private static Genre genre(Long id, String name) {
        Genre genre = new Genre();
        genre.setId(id);
        genre.setName(name);
        return genre;
    }
}
