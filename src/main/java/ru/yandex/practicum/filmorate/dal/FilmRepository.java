package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
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
    private final DirectorRepository directorRepository;

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

        Optional<List<Director>> optDirector = Optional.ofNullable(film.getDirectors());
        if (optDirector.isPresent()) {
            Set<Director> directorSet = new HashSet<>(optDirector.get());
            String sql = """
                    INSERT INTO directors_films(film_id, director_id)
                    VALUES(?, ?)
                    """;
            for (Director director : directorSet) {
                jdbcTemplate.update(sql, director.getId(), film.getId());
            }

            film.setDirectors(directorRepository.getDirectorsByFilmId(film.getId()));
        }
        return film;
    }

    public Film get(long id) {
        Film film = jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, filmRowMapper, id);
        loadGenresForFilm(film);
        loadLikesForFilm(film);
        film.setDirectors(directorRepository.getDirectorsByFilmId(film.getId()));
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

        Optional<Set<Genre>> result = Optional.ofNullable(film.getGenres());
        if (result.isEmpty()) {
            List<Genre> genres = genreRepository.getGenresByFilmId(film.getId());
            film.setGenres(new HashSet<>(genres));
        }

        Optional<List<Director>> optDirector = Optional.ofNullable(film.getDirectors());
        if (optDirector.isPresent() && !optDirector.get().isEmpty()) {
            Set<Director> newDirectors = new HashSet<>(optDirector.get());
            String sql = """
                    INSERT INTO directors_films(film_id, director_id)
                    VALUES(?, ?)
                    """;
            for (Director director : newDirectors) {
                jdbcTemplate.update(sql, film.getId(), director.getId());
            }
            film.setDirectors(directorRepository.getDirectorsByFilmId(film.getId()));
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

    public List<Film> getPopularFilms(Integer count, Integer genreId, Integer year) {  // заменил метод Сергея
        StringBuilder queryBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();

        // Базовый запрос
        queryBuilder.append("""
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
        """);

        if (genreId != null) {
            queryBuilder.append(" INNER JOIN film_genre fg ON f.film_id = fg.film_id AND fg.genre_id = ? ");
            params.add(genreId);
        }

        boolean hasWhere = false;
        if (year != null) {
            queryBuilder.append(" WHERE EXTRACT(YEAR FROM f.release_date) = ? ");
            params.add(year);
            hasWhere = true;
        }

        // Сортируем по количеству лайков
        queryBuilder.append(" ORDER BY (SELECT COUNT(*) FROM likes l WHERE l.film_id = f.film_id) DESC ");

        // Добавляем LIMIT только если count указан и больше 0
        if (count != null && count > 0) {
            queryBuilder.append(" LIMIT ? ");
            params.add(count);
        }

        String query = queryBuilder.toString();
        List<Film> films = jdbcTemplate.query(query, filmRowMapper, params.toArray());

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            for (Film film : films) {
                loadLikesForFilm(film);
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

    public List<Film> getCommonFilms(long userId, long friendId) {
        String query = """
    SELECT DISTINCT
        f.film_id,
        f.name,
        f.description,
        f.release_date,
        f.duration,
        f.mpa_id,
        m.name as mpa_name,
        g.genre_id,
        g.name as genre_name
    FROM films f
    LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
    LEFT JOIN film_genre fg ON f.film_id = fg.film_id
    LEFT JOIN genres g ON fg.genre_id = g.genre_id
    WHERE f.film_id IN (
        SELECT film_id FROM likes WHERE user_id = ?
        INTERSECT
        SELECT film_id FROM likes WHERE user_id = ?
    )
    ORDER BY f.film_id, g.genre_id
    """;

        Map<Long, Film> filmMap = new LinkedHashMap<>();

        jdbcTemplate.query(query, rs -> {
            long filmId = rs.getLong("film_id");
            Film film = filmMap.get(filmId);

            if (film == null) {
                film = filmRowMapper.mapRow(rs, 0);
                film.setGenres(new LinkedHashSet<>());
                filmMap.put(filmId, film);
            }

            Integer genreId = rs.getInt("genre_id");
            if (genreId > 0 && !rs.wasNull()) {
                Genre genre = new Genre(
                        genreId,
                        rs.getString("genre_name")
                );
                film.getGenres().add(genre);
            }
        }, userId, friendId);

        List<Film> films = new ArrayList<>(filmMap.values());

        for (Film film : films) {
            loadLikesForFilm(film);
        }

        films.sort(Comparator.comparing(Film::getRate).reversed());

        return films;
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

    public List<Film> getAllFilmsByDirectorAndSortedBy(Long directorId, String sortRule) {
        if (sortRule == null) {
            throw new NotFoundException("Параметр для сортировки не задан.");
        }

        switch (sortRule) {
            case "year" -> {
                String sql = """
                SELECT
                    f.film_id,
                    f.name,
                    f.description,
                    f.release_date,
                    f.duration,
                    f.mpa_id,
                    m.name as mpa_name
                FROM films f
                INNER JOIN directors_films df ON f.film_id = df.film_id
                LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
                WHERE df.director_id = ?
                ORDER BY f.release_date""";

                return loadGenresAndDirectors(sql, directorId);
            }
            case "likes" -> {
                String sql = """
                SELECT
                    f.film_id,
                    f.name,
                    f.description,
                    f.release_date,
                    f.duration,
                    f.mpa_id,
                    m.name as mpa_name,
                    COUNT(l.like_id) AS likes_count
                FROM films f
                INNER JOIN directors_films df ON f.film_id = df.film_id
                LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
                LEFT JOIN likes l ON f.film_id = l.film_id
                WHERE df.director_id = ?
                GROUP BY
                    f.film_id,
                    f.name,
                    f.description,
                    f.release_date,
                    f.duration,
                    f.mpa_id,
                    m.name
                ORDER BY COUNT(l.like_id) DESC""";

                return loadGenresAndDirectors(sql, directorId);
            }
            default -> throw new NotFoundException("Такого параметра для сортировки не существует.");
        }
    }

    private List<Film> loadGenresAndDirectors(String sql, Long directorId) {
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, directorId);
        for (Film film : films) {
            List<Genre> genres = genreRepository.getGenresByFilmId(film.getId());
            film.setGenres(new HashSet<>(genres));
            film.setDirectors(directorRepository.getDirectorsByFilmId(film.getId()));
        }
        return films;
    }

    public List<Film> searchFilms(String query, String[] searchFields) {
        String searchPattern = "%" + query + "%";

        // Определяем, какие поля искать
        boolean searchByTitle = false;
        boolean searchByDirector = false;

        for (String field : searchFields) {
            String trimmedField = field.trim().toLowerCase();
            if (trimmedField.equals("title")) {
                searchByTitle = true;
            }
            if (trimmedField.equals("director")) {
                searchByDirector = true;
            }
        }

        List<Film> films;

        if (searchByTitle && searchByDirector) {
            // Поиск и по названию, и по режиссеру
            films = jdbcTemplate.query(SEARCH_BY_TITLE_AND_DIRECTOR_QUERY, filmRowMapper,
                    searchPattern, searchPattern);
        } else if (searchByTitle) {
            // Поиск только по названию
            films = jdbcTemplate.query(SEARCH_BY_TITLE_QUERY, filmRowMapper, searchPattern);
        } else if (searchByDirector) {
            // Поиск только по режиссеру
            films = jdbcTemplate.query(SEARCH_BY_DIRECTOR_QUERY, filmRowMapper, searchPattern);
        } else {
            // Если передали невалидный параметр (но это уже проверено в сервисе)
            films = Collections.emptyList();
        }

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            for (Film film : films) {
                loadLikesForFilm(film);
                film.setDirectors(directorRepository.getDirectorsByFilmId(film.getId()));
            }
        }

        return films;
    }

    private static final String SEARCH_BY_TITLE_QUERY = """
            SELECT DISTINCT
                f.film_id,
                f.name,
                f.description,
                f.release_date,
                f.duration,
                f.mpa_id,
                m.name as mpa_name,
                COUNT(l.like_id) as likes_count
            FROM films f
            LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
            LEFT JOIN likes l ON f.film_id = l.film_id
            WHERE LOWER(f.name) LIKE LOWER(?)
            GROUP BY 
                f.film_id,
                f.name,
                f.description,
                f.release_date,
                f.duration,
                f.mpa_id,
                m.name
            ORDER BY likes_count DESC
            """;

    private static final String SEARCH_BY_DIRECTOR_QUERY = """
            SELECT DISTINCT
                f.film_id,
                f.name,
                f.description,
                f.release_date,
                f.duration,
                f.mpa_id,
                m.name as mpa_name,
                COUNT(l.like_id) as likes_count
            FROM films f
            LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
            LEFT JOIN likes l ON f.film_id = l.film_id
            INNER JOIN directors_films df ON f.film_id = df.film_id
            INNER JOIN directors d ON df.director_id = d.id
            WHERE LOWER(d.name) LIKE LOWER(?)
            GROUP BY 
                f.film_id,
                f.name,
                f.description,
                f.release_date,
                f.duration,
                f.mpa_id,
                m.name
            ORDER BY likes_count DESC
            """;

    private static final String SEARCH_BY_TITLE_AND_DIRECTOR_QUERY = """
            SELECT DISTINCT
                f.film_id,
                f.name,
                f.description,
                f.release_date,
                f.duration,
                f.mpa_id,
                m.name as mpa_name,
                COUNT(l.like_id) as likes_count
            FROM films f
            LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
            LEFT JOIN likes l ON f.film_id = l.film_id
            LEFT JOIN directors_films df ON f.film_id = df.film_id
            LEFT JOIN directors d ON df.director_id = d.id
            WHERE LOWER(f.name) LIKE LOWER(?) 
               OR LOWER(d.name) LIKE LOWER(?)
            GROUP BY 
                f.film_id,
                f.name,
                f.description,
                f.release_date,
                f.duration,
                f.mpa_id,
                m.name
            ORDER BY likes_count DESC
            """;


}