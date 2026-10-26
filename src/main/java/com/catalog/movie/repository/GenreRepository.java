package com.catalog.movie.repository;

import com.catalog.movie.entities.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    boolean existsByNameIgnoreCase(String name);
}
