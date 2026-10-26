package com.catalog.movie.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Director {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String nationality;
    private Integer birthYear;

    @OneToMany(mappedBy = "director")
    private List<Movie> movies = new ArrayList<>();
}