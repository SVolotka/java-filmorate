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
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class FilmRepository {

    private static final String FIND_ALL_QUERY = """
                SELECT
                    f.film_id,
                    f.name,
                    f.description,
                    f.release_date,
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
                    f.release_date,
                    f.duration,
                    f.mpa_id,
                    m.name as mpa_name
                FROM films f
                LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
                WHERE f.film_id = ?
            """;

    private static final String INSERT_QUERY =
            "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_QUERY = """
                UPDATE films
                SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ?
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
                f.release_date,
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
            Set<Integer> uniqueGenreIds = new HashSet<>(film.getGenreIds());
            List<Genre> existingGenres = genreRepository.findAllById(uniqueGenreIds);

            if (existingGenres.size() != uniqueGenreIds.size()) {
                Set<Integer> existingIds = existingGenres.stream()
                        .map(Genre::getId)
                        .collect(Collectors.toSet());

                Set<Integer> notFoundIds = new HashSet<>(uniqueGenreIds);
                notFoundIds.removeAll(existingIds);

                throw new NotFoundException("Жанры с id=" + notFoundIds + " не найдены");
            }
        }

        int rowsAffected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(film.getReleaseDate().atStartOfDay()));
            ps.setLong(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        if (rowsAffected == 0) {
            throw new InternalServerException("Не удалось создать фильм");
        }

        Long id = keyHolder.getKeyAs(Long.class);
        if (id == null) {
            throw new InternalServerException("Не удалось получить ID созданного фильма");
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
            Set<Integer> uniqueGenreIds = new HashSet<>(film.getGenreIds());

            List<Genre> existingGenres = genreRepository.findAllById(uniqueGenreIds);

            if (existingGenres.size() != uniqueGenreIds.size()) {
                Set<Integer> existingIds = existingGenres.stream()
                        .map(Genre::getId)
                        .collect(Collectors.toSet());

                Set<Integer> notFoundIds = new HashSet<>(uniqueGenreIds);
                notFoundIds.removeAll(existingIds);

                throw new NotFoundException("Жанры с id=" + notFoundIds + " не найдены");
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

    public List<Film> getPopularFilms(int count, Integer genreId, Integer year) {
        StringBuilder queryBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();

        // Базовый запрос с подзапросом для подсчета лайков
        queryBuilder.append("""
        SELECT
            f.film_id,
            f.name,
            f.description,
            f.release_date,
            f.duration,
            f.mpa_id,
            m.name as mpa_name,
            COALESCE(l.like_count, 0) as like_count
        FROM films f
        LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
        LEFT JOIN (
            SELECT film_id, COUNT(*) as like_count
            FROM likes
            GROUP BY film_id
        ) l ON f.film_id = l.film_id
        """);

        // Добавляем JOIN для фильтрации по жанру если нужно
        if (genreId != null) {
            queryBuilder.append(" INNER JOIN film_genre fg ON f.film_id = fg.film_id AND fg.genre_id = ? ");
            params.add(genreId);
        }

        // Добавляем WHERE для фильтрации по году если нужно
        if (year != null) {
            queryBuilder.append(" WHERE EXTRACT(YEAR FROM f.release_date) = ? ");
            params.add(year);
        }

        // Сортируем по количеству лайков (убывание), затем по ID
        queryBuilder.append(" ORDER BY COALESCE(l.like_count, 0) DESC, f.film_id ");

        // Лимит
        queryBuilder.append(" LIMIT ? ");
        params.add(count);

        String query = queryBuilder.toString();
        List<Film> films = jdbcTemplate.query(query, filmRowMapper, params.toArray());

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            // Устанавливаем количество лайков из результата запроса
            for (Film film : films) {
                // Количество лайков уже в like_count из запроса
                // Нужно передать его в film.setRate()
            }
        }

        return films;
    }

    public void loadGenresForFilm(Film film) {
        if (film == null || film.getId() == null) {
            return;
        }

        List<Genre> genres = jdbcTemplate.query(
                FIND_GENRES_BY_FILM_ID,
                genreRowMapper,
                film.getId()
        );
        film.setGenres(new LinkedHashSet<>(genres));
    }

    public void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .filter(Objects::nonNull)
                .toList();

        if (filmIds.isEmpty()) {
            return;
        }

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
        if (film == null || film.getId() == null) {
            return;
        }

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

    public List<Film> searchByTitle(String query) {
        String searchPattern = "%" + query.toLowerCase() + "%";

        String searchQuery = FIND_ALL_QUERY +
                " WHERE LOWER(f.name) LIKE ? " +
                " ORDER BY (SELECT COUNT(*) FROM likes l WHERE l.film_id = f.film_id) DESC";

        List<Film> films = jdbcTemplate.query(searchQuery, filmRowMapper, searchPattern);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

        return films;
    }
}