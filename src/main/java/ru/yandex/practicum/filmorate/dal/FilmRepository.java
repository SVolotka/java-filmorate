package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Repository
@Slf4j
public class FilmRepository {

    private static final String FIND_ALL_QUERY = """
                SELECT
                    f.film_id,
                    f.name,
                    f.description,
                    f.releaseDate,
                    f.duration,
                    f.mpa_id,
                    m.name as mpa_name
                FROM films f
                LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
            """;

    private static final String FIND_BY_ID_QUERY = """
                SELECT
                    f.film_id,
                    f.name,
                    f.description,
                    f.releaseDate,
                    f.duration,
                    f.mpa_id,
                    m.name as mpa_name
                FROM films f
                LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
                WHERE f.film_id = ?
            """;

    private static final String INSERT_QUERY =
            "INSERT INTO films (name, description, releaseDate, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_QUERY = """
                UPDATE films
                SET name = ?, description = ?, releaseDate = ?, duration = ?, mpa_id = ?
                WHERE film_id = ?
            """;

    private static final String ADD_LIKE_QUERY = """
            INSERT INTO likes (user_id, film_id)
            SELECT ?, ? WHERE NOT EXISTS (
                SELECT 1 FROM likes WHERE user_id = ? AND film_id = ?
            )
            """;

    private static final String REMOVE_LIKE_QUERY = "DELETE FROM likes WHERE user_id = ? AND film_id = ?";

    private static final String GET_LIKES_COUNT_QUERY = "SELECT COUNT(*) FROM likes WHERE film_id = ?";
    private static final String GET_LIKED_USERS_QUERY = "SELECT user_id FROM likes WHERE film_id = ?";

    private static final String GET_POPULAR_FILMS_QUERY = """
            SELECT
                f.film_id,
                f.name,
                f.description,
                f.releaseDate,
                f.duration,
                f.mpa_id,
                m.name AS mpa_name
            FROM films f
            LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
            ORDER BY (
                SELECT COUNT(*)
                FROM likes l
                WHERE l.film_id = f.film_id
            ) DESC
            LIMIT ?
            """;

    private static final String FIND_GENRES_BY_FILM_ID = """
    SELECT g.genre_id, g.name
    FROM film_genre fg
    JOIN genres g ON fg.genre_id = g.genre_id
    WHERE fg.film_id = ?
    ORDER BY g.genre_id
    """;

    private static final String DELETE_GENRES_BY_FILM_ID = "DELETE FROM film_genre WHERE film_id = ?";
    private static final String INSERT_FILM_GENRE = "INSERT INTO film_genre (film_id, genre_id) VALUES (?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final GenreRowMapper genreRowMapper;
    private final MpaRepository mpaRepository;
    private final GenreRepository genreRepository;

    public FilmRepository(JdbcTemplate jdbcTemplate,
                          FilmRowMapper filmRowMapper,
                          GenreRowMapper genreRowMapper,
                          MpaRepository mpaRepository,
                          GenreRepository genreRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.filmRowMapper = filmRowMapper;
        this.genreRowMapper = genreRowMapper;
        this.mpaRepository = mpaRepository;
        this.genreRepository = genreRepository;
    }

    public List<Film> findAll() {
        List<Film> films = jdbcTemplate.query(FIND_ALL_QUERY, filmRowMapper);
        loadGenresForFilms(films);
        for (Film film : films) {
            loadLikesForFilm(film);
        }
        return films;
    }

    public Film create(Film film) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("Рейтинг MPA обязателен");
        }
        if (mpaRepository.findById(film.getMpa().getId()).isEmpty()) {
            throw new NotFoundException("Рейтинг MPA с id=" + film.getMpa().getId() + " не найден");
        }

        if (film.getGenreIds() != null && !film.getGenreIds().isEmpty()) {
            for (Integer genreId : film.getGenreIds()) {
                if (genreRepository.findById(genreId).isEmpty()) {
                    throw new NotFoundException("Жанр с id=" + genreId + " не найден");
                }
            }
        }

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(film.getReleaseDate().atStartOfDay()));
            ps.setLong(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKeyAs(Long.class);
        if (id == null) {
            throw new InternalServerException("Не удалось сохранить данные");
        }
        film.setId(id);

        updateGenres(id, film.getGenreIds());

        return film;
    }

    public Film get(long id) {
        Film film = jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, filmRowMapper, id);
        loadGenresForFilm(film);
        loadLikesForFilm(film);
        return film;
    }

    public Film update(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("Рейтинг MPA обязателен");
        }
        if (mpaRepository.findById(film.getMpa().getId()).isEmpty()) {
            throw new NotFoundException("Рейтинг MPA с id=" + film.getMpa().getId() + " не найден");
        }

        if (film.getGenreIds() != null && !film.getGenreIds().isEmpty()) {
            for (Integer genreId : film.getGenreIds()) {
                if (genreRepository.findById(genreId).isEmpty()) {
                    throw new NotFoundException("Жанр с id=" + genreId + " не найден");
                }
            }
        }

        int rowsUpdated = jdbcTemplate.update(UPDATE_QUERY,
                film.getName(),
                film.getDescription(),
                Timestamp.valueOf(film.getReleaseDate().atStartOfDay()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        if (rowsUpdated == 0) {
            throw new InternalServerException("Не удалось обновить данные");
        }

        updateGenres(film.getId(), film.getGenreIds());

        return film;
    }

    public void addLike(long filmId, long userId) {
        jdbcTemplate.update(ADD_LIKE_QUERY, userId, filmId, userId, filmId);
    }

    public void removeLike(long filmId, long userId) {
        int rowsDeleted = jdbcTemplate.update(REMOVE_LIKE_QUERY, userId, filmId);
        if (rowsDeleted == 0) {
            log.debug("Лайк не найден для удаления: filmId={}, userId={}", filmId, userId);
        }
    }

    public List<Film> getPopularFilms(int count) {
        List<Film> films = jdbcTemplate.query(GET_POPULAR_FILMS_QUERY, filmRowMapper, count);

        if (films.isEmpty()) return films;

        loadGenresForFilms(films);
        List<Long> filmIds = films.stream().map(Film::getId).toList();

        String inClause = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String countQuery = "SELECT film_id, COUNT(*) AS like_count FROM likes WHERE film_id IN (" + inClause + ") GROUP BY film_id";

        Map<Long, Long> likesCountMap = jdbcTemplate.query(countQuery, rs -> {
            Map<Long, Long> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getLong("film_id"), rs.getLong("like_count"));
            }
            return map;
        }, filmIds.toArray());

        String usersQuery = "SELECT film_id, user_id FROM likes WHERE film_id IN (" + inClause + ")";
        Map<Long, Set<Long>> likedUsersMap = new HashMap<>();
        jdbcTemplate.query(usersQuery, rs -> {
            while (rs.next()) {
                long filmId = rs.getLong("film_id");
                long userId = rs.getLong("user_id");
                likedUsersMap.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
            }
        }, filmIds.toArray());

        for (Film film : films) {
            film.setRate(likesCountMap.getOrDefault(film.getId(), 0L));
            film.setUserIds(likedUsersMap.getOrDefault(film.getId(), new HashSet<>()));
        }

        return films;
    }

    public void loadGenresForFilm(Film film) {
        if (film == null || film.getId() == null) return;

        List<Genre> genres = jdbcTemplate.query(
                FIND_GENRES_BY_FILM_ID,
                genreRowMapper,
                film.getId()
        );
        film.setGenres(new LinkedHashSet<>(genres));
    }

    public void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .filter(Objects::nonNull)
                .toList();

        if (filmIds.isEmpty()) return;

        String inClause = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String query = """
        SELECT fg.film_id, g.genre_id, g.name
        FROM film_genre fg
        JOIN genres g ON fg.genre_id = g.genre_id
        WHERE fg.film_id IN (%s)
        ORDER BY fg.film_id, g.genre_id
        """.formatted(inClause);

        Map<Long, Set<Genre>> genresByFilmId = new HashMap<>();
        jdbcTemplate.query(query, rs -> {
            while (rs.next()) {
                long filmId = rs.getLong("film_id");
                Genre genre = genreRowMapper.mapRow(rs, 0);
                genresByFilmId.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
            }
        }, filmIds.toArray());

        for (Film film : films) {
            film.setGenres(genresByFilmId.getOrDefault(film.getId(), new LinkedHashSet<>()));
        }
    }

    public void updateGenres(Long filmId, Set<Integer> genreIds) {
        jdbcTemplate.update(DELETE_GENRES_BY_FILM_ID, filmId);
        if (genreIds != null && !genreIds.isEmpty()) {
            for (Integer genreId : genreIds) {
                jdbcTemplate.update(INSERT_FILM_GENRE, filmId, genreId);
            }
        }
    }

    private void loadLikesForFilm(Film film) {
        if (film == null || film.getId() == null) return;

        Long count = jdbcTemplate.queryForObject(
                GET_LIKES_COUNT_QUERY,
                Long.class,
                film.getId()
        );
        film.setRate(count != null ? count : 0L);

        List<Long> userIds = jdbcTemplate.queryForList(
                GET_LIKED_USERS_QUERY,
                Long.class,
                film.getId()
        );
        film.setUserIds(new HashSet<>(userIds));
    }

    public boolean exists(long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}