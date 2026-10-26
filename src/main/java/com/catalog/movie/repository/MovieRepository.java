package com.catalog.movie.repository;

import com.catalog.movie.entities.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    @EntityGraph(attributePaths = {"director", "genres"})
    Optional<Movie> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = "director")
    Page<Movie> findByDirectorId(Long directorId, Pageable pageable);

    boolean existsByDirectorId(Long directorId);

    /**
     * Lists movies, optionally filtered by release year and/or genre name (case-insensitive).
     * The genre filter uses a subquery so each movie appears once and keeps all its genres.
     */
    @EntityGraph(attributePaths = "director")
    @Query("""
            select m from Movie m
            where (:year is null or m.releaseYear = :year)
              and (:genre is null or exists (
                    select 1 from Movie fm join fm.genres g
                    where fm = m and lower(g.name) = lower(:genre)))
            """)
    Page<Movie> search(@Param("genre") String genre, @Param("year") Integer year, Pageable pageable);
}
