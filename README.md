# Movie Catalog Service

A Spring Boot REST API that manages **directors**, **movies** and **genres**:
- a director has many movies, and every movie has exactly one director
- a movie can have several genres, stored in the `movie_genre` join table

Data is kept in an in-memory H2 database, so there's nothing to install or configure.

**Stack:** Java 17 · Spring Boot 4.1 · Spring Data JPA · H2 · Bean Validation · springdoc-openapi · JUnit 5 / Mockito / MockMvc

---

## Run it

**Option 1: Docker** (no Java needed)
```bash
docker build -t movie-catalog .
docker run --rm -p 8080:8080 movie-catalog
```

**Option 2: Locally** (JDK 17+)
```bash
./mvnw spring-boot:run
```

Once the app is up:

| What | URL |
|---|---|
| Swagger UI (try every endpoint here) | http://localhost:8080/swagger-ui.html |
| App info | http://localhost:8080/api/info |
| H2 console | http://localhost:8080/h2-console. Set **JDBC URL** to `jdbc:h2:mem:moviedb`, user `sa`, empty password. |
| Generated OpenAPI spec | http://localhost:8080/v3/api-docs |

The database starts empty on every run.

## Run the tests

```bash
./mvnw test
```

There are 43 tests:
- 34 Mockito unit tests for the service layer
- 8 MockMvc integration tests that call the API over HTTP against H2
- 1 context-load test

---

## API

The full contract is in [`openapi.yaml`](openapi.yaml), which is OpenAPI 3.0. Swagger UI shows the same endpoints.

| Method | Endpoint | Description | Success |
|---|---|---|---|
| GET | `/api/info` | App name and version | 200 |
| POST | `/api/directors` | Create a director | 201 |
| GET | `/api/directors` | List directors | 200 |
| GET | `/api/directors/{id}` | Get a director | 200 |
| PUT | `/api/directors/{id}` | Update a director | 200 |
| DELETE | `/api/directors/{id}` | Delete a director | 204 |
| GET | `/api/directors/{id}/movies` | Movies by a director | 200 |
| POST | `/api/genres` | Create a genre | 201 |
| GET | `/api/genres` | List genres | 200 |
| GET | `/api/genres/{id}` | Get a genre | 200 |
| POST | `/api/movies` | Create a movie (with `genreIds`) | 201 |
| GET | `/api/movies?genre=&year=` | List movies, optionally filtered by genre name and/or year | 200 |
| GET | `/api/movies/{id}` | Get a movie with its director and genres | 200 |
| PUT | `/api/movies/{id}` | Update a movie | 200 |
| DELETE | `/api/movies/{id}` | Delete a movie | 204 |
| POST | `/api/movies/{id}/genres/{gid}` | Add a genre to a movie | 200 |
| DELETE | `/api/movies/{id}/genres/{gid}` | Remove a genre from a movie | 200 |

**Paging:** list endpoints accept these query parameters:
- `page`: zero-based
- `size`: default 20, max 100
- `sort`: `field,asc|desc`, default `id`

The fields you can sort by are:
- directors: `id`, `name`, `nationality`, `birthYear`
- genres: `id`, `name`
- movies: `id`, `title`, `releaseYear`, `rating`

### Try it

These commands, run in order against a fresh start, will work as written:

```bash
curl -X POST localhost:8080/api/directors -H 'Content-Type: application/json' \
  -d '{"name":"Christopher Nolan","nationality":"British","birthYear":1970}'

curl -X POST localhost:8080/api/genres -H 'Content-Type: application/json' -d '{"name":"Sci-Fi"}'
curl -X POST localhost:8080/api/genres -H 'Content-Type: application/json' -d '{"name":"Thriller"}'

# no rating sent → catalog.default-rating (7.0) is applied
curl -X POST localhost:8080/api/movies -H 'Content-Type: application/json' \
  -d '{"title":"Inception","releaseYear":2010,"directorId":1,"genreIds":[1]}'

curl 'localhost:8080/api/movies?genre=sci-fi&year=2010'
curl -X POST localhost:8080/api/movies/1/genres/2
curl localhost:8080/api/directors/1/movies
```

---

## Approach

- **Layers:** controller → service interface → service implementation → Spring Data repository.
  - Controllers only handle HTTP.
  - All business rules live in the services.
- **DTOs:** requests and responses are Java records, and entities never leave the service layer.
  - `CatalogMapper` converts entities to responses.
  - Every list endpoint returns the same `PageResponse` shape.
- **Configuration:**
  - `app.*` and `catalog.*` are bound to `@ConfigurationProperties` records, so nothing is hardcoded.
  - Dependencies use constructor injection.
- **Errors:** a single `@RestControllerAdvice` turns every exception into the same JSON error body with the right status code.
- **Persistence:**
  - `@Transactional` sits on each service method (read-only for reads), and `open-in-view` is disabled.
  - `@EntityGraph` and Hibernate batch fetching avoid N+1 queries when movies are listed with their director and genres.

## Validation and business rules

| Rule | Result |
|---|---|
| Director `name`, genre `name`, movie `title`, `releaseYear` and `directorId` are required | 400 with per-field messages |
| `rating` must be between 0.0 and 10.0, or left out | 400 |
| `rating` left out | `catalog.default-rating` is applied |
| More genres than `catalog.max-genres-per-movie`, on create, update or when adding a genre | 400 |
| Unknown director, movie or genre id, including ids in the request body | 404 |
| Genre name already exists (case-insensitive) | 409 |
| Deleting a director who still has movies | 409 (delete or reassign the movies first) |
| Adding a genre the movie already has | 200, no change |
| Removing a genre the movie doesn't have | 404 |
| `PUT /api/movies/{id}` without `genreIds` | The movie keeps its current genres |
| Malformed JSON, non-numeric id, unknown sort field | 400 |

Every error has the same shape. `fieldErrors` only appears for validation errors.

```json
{
  "timestamp": "2026-10-24T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/movies",
  "fieldErrors": { "title": "title is required" }
}
```

## Configuration

All settings are in `src/main/resources/application.properties`:

| Property | Default | Purpose |
|---|---|---|
| `app.name` | `movie_catalog` | Shown by `/api/info` |
| `app.version` | `1.0` | Shown by `/api/info` |
| `catalog.max-genres-per-movie` | `3` | Most genres a movie can have |
| `catalog.default-rating` | `7.0` | Rating used when none is sent |

You can override any of these without rebuilding. Either set an environment variable on the container, or pass the property as an argument to the jar:
```bash
docker run --rm -p 8080:8080 -e CATALOG_MAX_GENRES_PER_MOVIE=5 -e APP_VERSION=2.0 movie-catalog
java -jar target/movie-catalog-0.0.1-SNAPSHOT.jar --catalog.default-rating=6.5
```

The Dockerfile sets `SPRING_H2_CONSOLE_SETTINGS_WEB_ALLOW_OTHERS=true`. Without it, H2 rejects console requests that come in through Docker's network.