package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
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

    private static final String FIND_GENRES_BY_FILM_ID = """
            SELECT g.genre_id, g.name
            FROM genres g
            INNER JOIN film_genre fg ON g.genre_id = fg.genre_id
            WHERE fg.film_id = ?
            ORDER BY g.genre_id
            """;

    private static final String DELETE_GENRES_BY_FILM_ID = "DELETE FROM film_genre WHERE film_id = ?";
    private static final String INSERT_FILM_GENRE = "INSERT INTO film_genre (film_id, genre_id) VALUES (?, ?)";
    private static final String DELETE_FILM_BY_ID_QUERY = "DELETE FROM films WHERE film_id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final MpaRepository mpaRepository;
    private final GenreRepository genreRepository;
    private final DirectorRepository directorRepository;

    public List<Film> findAll() {
        String query = """
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

        List<Film> films = jdbcTemplate.query(query, filmRowMapper);

        loadLikesForFilms(films);
        loadGenresForFilms(films);
        loadDirectorsForFilms(films);
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
        validateAndSaveDirectors(film);

        return film;
    }

    public Film get(long id) {
        Film film;
        try {
             film = jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, filmRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Фильм с id = " + id + " отсутствует в БД");
        }

        loadGenresForFilm(film);
        loadLikesForFilm(film);
        film.setDirectors(directorRepository.getDirectorsByFilmId(film.getId()));
        return film;
    }

    public Film update(Film film) {
        if (!exists(film.getId())) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " отсутствует в БД");
        }

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

        String deleteDirectorsQuery = "DELETE FROM directors_films WHERE film_id = ?";
        jdbcTemplate.update(deleteDirectorsQuery, film.getId());
        Map<Long, Director> directors = directorRepository.getAllDirectors();
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            Set<Director> newDirectors = new HashSet<>(film.getDirectors());
            for (Director director : newDirectors) {
                if (!directors.containsKey(director.getId())) {
                    throw new NotFoundException("Режиссер с id=" + director.getId() + " не найден");
                }
            }

            String insertDirectorQuery = "INSERT INTO directors_films(film_id, director_id) VALUES(?, ?)";
            for (Director director : newDirectors) {
                jdbcTemplate.update(insertDirectorQuery, film.getId(), director.getId());
            }
            film.setDirectors(new ArrayList<>(newDirectors));
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
        film.setDirectors(directorRepository.getDirectorsByFilmId(film.getId()));

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

    public List<Film> getPopularFilms(Integer count, Integer genreId, Integer year) {
        StringBuilder queryBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();

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

        if (year != null) {
            queryBuilder.append(" WHERE EXTRACT(YEAR FROM f.release_date) = ? ");
            params.add(year);
        }

        queryBuilder.append(" ORDER BY (SELECT COUNT(*) FROM likes l WHERE l.film_id = f.film_id) DESC ");

        if (count != null && count > 0) {
            queryBuilder.append(" LIMIT ? ");
            params.add(count);
        }

        String query = queryBuilder.toString();
        List<Film> films = jdbcTemplate.query(query, filmRowMapper, params.toArray());

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadLikesForFilms(films);
            loadDirectorsForFilms(films);
        }

        return films;
    }

    public void loadGenresForFilms(List<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        List<Long> filmIds = films.stream()
                .filter(film -> film.getId() != null)
                .map(Film::getId)
                .distinct()
                .collect(Collectors.toList());

        if (filmIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String findGenresByFilmIdsSql = String.format("""
            SELECT fg.film_id, g.genre_id, g.name
            FROM genres g
            INNER JOIN film_genre fg ON g.genre_id = fg.genre_id
            WHERE fg.film_id IN (%s)
            ORDER BY fg.film_id, g.genre_id
            """, placeholders);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                findGenresByFilmIdsSql,
                filmIds.toArray()
        );

        Map<Long, Set<Genre>> filmGenresMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long filmId = ((Number) row.get("film_id")).longValue();
            Genre genre = new Genre(
                    ((Number) row.get("genre_id")).intValue(),
                    (String) row.get("name")
            );
            filmGenresMap.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        }

        for (Film film : films) {
            if (film.getId() != null) {
                Set<Genre> genres = filmGenresMap.get(film.getId());
                film.setGenres(genres != null ? genres : new LinkedHashSet<>());
            } else {
                film.setGenres(new LinkedHashSet<>());
            }
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
        loadLikesForFilms(films);

        films.sort(Comparator.comparing(Film::getRate).reversed());

        return films;
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

        if (!directorRepository.existsById(directorId)) {
            throw new NotFoundException("Режиссер с id=" + directorId + " не найден");
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

    public void deleteFilmById(long filmId) {
        int rowsAffected = jdbcTemplate.update(DELETE_FILM_BY_ID_QUERY, filmId);
        if (rowsAffected == 0) {
            throw new NotFoundException(String.format("Фильм с id: %s не найден", filmId));
        }
    }

    public List<Film> searchFilms(String query, Set<String> searchBy) {
        String likePattern = "%" + query + "%";
        List<Object> params = new ArrayList<>();

        StringBuilder sqlBuilder = new StringBuilder("""
        SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
               f.mpa_id, m.name AS mpa_name
        FROM films f
        LEFT JOIN mpa_rating m ON f.mpa_id = m.rating_id
        """);

        List<String> whereConditions = new ArrayList<>();

        if (searchBy.contains("title")) {
            whereConditions.add("LOWER(f.name) LIKE ?");
            params.add(likePattern);
        }

        if (searchBy.contains("director")) {
            sqlBuilder.append("""
            LEFT JOIN directors_films df ON f.film_id = df.film_id
            LEFT JOIN directors d ON df.director_id = d.id
            """);
            whereConditions.add("LOWER(d.name) LIKE ?");
            params.add(likePattern);
        }
        if (!whereConditions.isEmpty()) {
            sqlBuilder.append(" WHERE ").append(String.join(" OR ", whereConditions));
        }

        sqlBuilder.append("""
        ORDER BY (
            SELECT COUNT(*)
            FROM likes l
            WHERE l.film_id = f.film_id
        ) DESC
        """);

        String sql = sqlBuilder.toString();
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, params.toArray());
        loadLikesForFilms(films);
        loadGenresForFilms(films);
        loadDirectorsForFilms(films);

        return films;
    }

    public List<Film> getRecommendedFilms(long userId) {
        String checkOnExistUser = "SELECT user_id FROM likes WHERE user_id = ?";
        List<Long> check = jdbcTemplate.query(checkOnExistUser, new Object[]{userId}, (rs, rowNum) -> rs.getLong("user_id"));

        if (check.isEmpty()) {
            return Collections.emptyList();
        }

        String getFilmIdByUser = "SELECT film_id FROM likes WHERE user_id = ?";
        List<Long> lisTOfFilmsByUserId = jdbcTemplate.query(getFilmIdByUser, new Object[]{userId}, (rs, rowNum) -> rs.getLong("film_id"));

        String getFilmIdByOtherUsers = "SELECT user_id, film_id FROM likes WHERE user_id != ? ORDER BY user_id";
        Map<Long, List<Long>> listOfFilmsOtherUsers = jdbcTemplate.query(getFilmIdByOtherUsers, new Object[]{userId}, rs -> {
            Map<Long, List<Long>> result = new HashMap<>();
            while (rs.next()) {
                Long user = rs.getLong("user_id");
                Long film = rs.getLong("film_id");

                result.computeIfAbsent(user, k -> new ArrayList<>()).add(film);
            }
            return result;
        });

        Map<Long, Long> usersIdAndNumberOfMatches = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : listOfFilmsOtherUsers.entrySet()) {
            List<Long> intersection = entry.getValue().stream()
                    .filter(lisTOfFilmsByUserId::contains)
                    .toList();

            if (!intersection.isEmpty()) {
                usersIdAndNumberOfMatches.put(entry.getKey(), (long) intersection.size());
            }
        }

        if (usersIdAndNumberOfMatches.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> usersIdWithMaxNumberOfMatches = usersIdAndNumberOfMatches.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(1)
                .toList();

        Set<Long> set1 = new HashSet<>(lisTOfFilmsByUserId);
        Set<Long> set2 = new HashSet<>(listOfFilmsOtherUsers.get(usersIdWithMaxNumberOfMatches.getFirst()));

        Set<Long> difference = set2.stream()
                .filter(e -> !set1.contains(e))
                .collect(Collectors.toSet());

        if (difference.isEmpty()) {
            return Collections.emptyList();
        }

        String sqlQueryGetRecommendedFilms = """
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
                WHERE f.film_id IN (?)
                """;

        List<Film> recommendedFilms = jdbcTemplate.query(sqlQueryGetRecommendedFilms, filmRowMapper, difference.toArray());

        loadLikesForFilms(recommendedFilms);
        loadGenresForFilms(recommendedFilms);
        loadDirectorsForFilms(recommendedFilms);

        return recommendedFilms;
    }

    private void loadLikesForFilm(Film film) {
        if (film == null || film.getId() == null) {
            return;
        }

        try {
            Long count = jdbcTemplate.queryForObject(
                    GET_LIKES_COUNT_QUERY,
                    Long.class,
                    film.getId()
            );
            film.setRate(count != null ? count : 0L);
        } catch (EmptyResultDataAccessException e) {
            film.setRate(0L);
        }

        List<Long> userIds = jdbcTemplate.queryForList(
                GET_LIKED_USERS_QUERY,
                Long.class,
                film.getId()
        );
        film.setUserIds(new HashSet<>(userIds));
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

    private void loadGenresForFilm(Film film) {
        if (film == null || film.getId() == null) {
            return;
        }

        List<Genre> genres = jdbcTemplate.query(FIND_GENRES_BY_FILM_ID,
                (rs, rowNum) -> new Genre(rs.getInt("genre_id"), rs.getString("name")),
                film.getId());
        film.setGenres(new LinkedHashSet<>(genres));
    }

    private void loadLikesForFilms(Collection<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        List<Long> filmIds = films.stream()
                .filter(film -> film.getId() != null)
                .map(Film::getId)
                .distinct()
                .collect(Collectors.toList());

        if (filmIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String getLikesCountByFilmIdsQuery = String.format("""
        SELECT film_id, COUNT(*) as count
        FROM likes
        WHERE film_id IN (%s)
        GROUP BY film_id
        """, placeholders);

        String getLikedUsersByFilmIdsQuery = String.format("""
        SELECT film_id, user_id
        FROM likes
        WHERE film_id IN (%s)
        ORDER BY film_id
        """, placeholders);

        Map<Long, Long> likesCountByFilmId = new HashMap<>();
        jdbcTemplate.query(getLikesCountByFilmIdsQuery, filmIds.toArray(), rs -> {
            long filmId = rs.getLong("film_id");
            long count = rs.getLong("count");
            likesCountByFilmId.put(filmId, count);
        });

        Map<Long, Set<Long>> likedUsersByFilmId = new HashMap<>();
        jdbcTemplate.query(getLikedUsersByFilmIdsQuery, filmIds.toArray(), rs -> {
            long filmId = rs.getLong("film_id");
            long userId = rs.getLong("user_id");
            likedUsersByFilmId.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        });

        for (Film film : films) {
            if (film.getId() != null) {
                Long count = likesCountByFilmId.get(film.getId());
                film.setRate(count != null ? count : 0L);

                Set<Long> userIds = likedUsersByFilmId.get(film.getId());
                film.setUserIds(userIds != null ? userIds : new HashSet<>());
            } else {
                film.setRate(0L);
                film.setUserIds(new HashSet<>());
            }
        }
    }

    private void validateAndSaveDirectors(Film film) {
        List<Long> directorIds = film.getDirectors().stream()
                .map(Director::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (directorIds.size() != film.getDirectors().size()) {
            throw new NotFoundException("Некоторые режиссеры имеют null ID");
        }

        List<Long> existingIds = directorRepository.findAllExistingIds(directorIds);
        if (existingIds.size() != directorIds.size()) {
            Set<Long> existingSet = new HashSet<>(existingIds);
            List<Long> notFoundIds = directorIds.stream()
                    .filter(id -> !existingSet.contains(id))
                    .toList();
            throw new NotFoundException("Режиссеры с id=" + notFoundIds + " не найдены");
        }


        String insertQuery = "INSERT INTO directors_films (film_id, director_id) VALUES (?, ?)";
        List<Object[]> batchArgs = film.getDirectors().stream()
                .map(director -> new Object[]{film.getId(), director.getId()})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(insertQuery, batchArgs);
    }

    public void loadDirectorsForFilms(List<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        Map<Long, List<Director>> directorsByFilmId = directorRepository.getDirectorsForFilms(films);

        for (Film film : films) {
            if (film.getId() != null) {
                List<Director> directors = directorsByFilmId.get(film.getId());
                film.setDirectors(directors != null ? directors : new ArrayList<>());
            } else {
                film.setDirectors(new ArrayList<>());
            }
        }
    }


}