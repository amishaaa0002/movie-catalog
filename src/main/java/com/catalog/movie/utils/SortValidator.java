package com.catalog.movie.utils;

import com.catalog.movie.exception.BusinessRuleException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Rejects sort properties that are not part of the public API,
 * so {@code ?sort=bogus} is a 400 instead of a query failure.
 */
public final class SortValidator {

    public static final Set<String> DIRECTOR_SORT = Set.of("id", "name", "nationality", "birthYear");
    public static final Set<String> GENRE_SORT = Set.of("id", "name");
    public static final Set<String> MOVIE_SORT = Set.of("id", "title", "releaseYear", "rating");

    private SortValidator() {
    }

    public static Pageable validate(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new BusinessRuleException("Invalid sort property '" + order.getProperty()
                        + "'. Allowed: " + allowed.stream().sorted().toList());
            }
        }
        return pageable;
    }
}
