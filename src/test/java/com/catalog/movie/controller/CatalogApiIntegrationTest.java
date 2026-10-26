package com.catalog.movie.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the HTTP layer against the in-memory H2 database.
 * Config values are overridden here to prove they are read from properties, not hardcoded.
 */
@SpringBootTest(properties = {
        "app.version=9.9-test",
        "catalog.max-genres-per-movie=2",
        "catalog.default-rating=5.5"
})
@AutoConfigureMockMvc
@Transactional
class CatalogApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void infoEndpointExposesConfiguredNameAndVersion() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("movie_catalog"))
                .andExpect(jsonPath("$.version").value("9.9-test"));
    }

    @Test
    void fullMovieLifecycle() throws Exception {
        long directorId = createDirector("Christopher Nolan");
        long sciFi = createGenre("Sci-Fi");
        long drama = createGenre("Drama");

        String body = postJson("/api/movies", """
                {"title":"Inception","releaseYear":2010,"directorId":%d,"genreIds":[%d]}
                """.formatted(directorId, sciFi))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/movies/")))
                .andExpect(jsonPath("$.rating").value(5.5))
                .andExpect(jsonPath("$.director.name").value("Christopher Nolan"))
                .andExpect(jsonPath("$.genres[0].name").value("Sci-Fi"))
                .andReturn().getResponse().getContentAsString();
        long movieId = ((Number) JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(post("/api/movies/{id}/genres/{gid}", movieId, drama))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres", hasSize(2)));

        mockMvc.perform(get("/api/movies").param("genre", "drama").param("year", "2010"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(movieId));

        mockMvc.perform(get("/api/directors/{id}/movies", directorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Inception"));

        mockMvc.perform(put("/api/movies/{id}", movieId).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Inception","releaseYear":2010,"rating":8.8,"directorId":%d}
                        """.formatted(directorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(8.8))
                .andExpect(jsonPath("$.genres", hasSize(2)));

        mockMvc.perform(delete("/api/movies/{id}/genres/{gid}", movieId, drama))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres", hasSize(1)));

        mockMvc.perform(delete("/api/directors/{id}", directorId))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/movies/{id}", movieId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/movies/{id}", movieId))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/directors/{id}", directorId))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidPayloadReturnsFieldErrors() throws Exception {
        postJson("/api/movies", """
                {"title":"","rating":11}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/movies"))
                .andExpect(jsonPath("$.fieldErrors.title").value("title is required"))
                .andExpect(jsonPath("$.fieldErrors.releaseYear").value("releaseYear is required"))
                .andExpect(jsonPath("$.fieldErrors.rating").value("rating must be between 0.0 and 10.0"))
                .andExpect(jsonPath("$.fieldErrors.directorId").value("directorId is required"));
    }

    @Test
    void tooManyGenresUsesConfiguredMaximum() throws Exception {
        long directorId = createDirector("Denis Villeneuve");
        long a = createGenre("Action");
        long b = createGenre("Thriller");
        long c = createGenre("Mystery");

        postJson("/api/movies", """
                {"title":"Dune","releaseYear":2021,"directorId":%d,"genreIds":[%d,%d,%d]}
                """.formatted(directorId, a, b, c))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A movie can have at most 2 genres, but 3 were supplied"));
    }

    @Test
    void duplicateGenreReturnsConflict() throws Exception {
        createGenre("Horror");

        postJson("/api/genres", """
                {"name":"HORROR"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Genre 'HORROR' already exists"));
    }

    @Test
    void unknownResourcesReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/directors/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Director not found with id 999999"));

        postJson("/api/movies", """
                {"title":"Ghost","releaseYear":2000,"directorId":999999}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Director not found with id 999999"));
    }

    @Test
    void badRequestsAreReportedAsClientErrors() throws Exception {
        mockMvc.perform(get("/api/movies/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));

        mockMvc.perform(get("/api/movies").param("sort", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort property 'bogus'")));

        postJson("/api/directors", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void listEndpointsArePaged() throws Exception {
        createGenre("Western");
        createGenre("Comedy");
        createGenre("Animation");

        mockMvc.perform(get("/api/genres").param("size", "2").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Animation"))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    private long createDirector(String name) throws Exception {
        String body = postJson("/api/directors", """
                {"name":"%s","nationality":"Canadian","birthYear":1967}
                """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mockMvc.perform(get("/api/directors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));
        return id;
    }

    private long createGenre(String name) throws Exception {
        String body = postJson("/api/genres", """
                {"name":"%s"}
                """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
