package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class GenreRepository {

    private final JdbcTemplate jdbcTemplate;
    private final GenreRowMapper genreRowMapper;

    private static final String FIND_ALL_QUERY =
            "SELECT genre_id, name FROM genres ORDER BY genre_id";

    private static final String FIND_BY_ID_QUERY =
            "SELECT genre_id, name FROM genres WHERE genre_id = ?";

    public List<Genre> findAll() {
        return jdbcTemplate.query(FIND_ALL_QUERY, genreRowMapper);
    }

    public Optional<Genre> findById(int id) {
        try {
            Genre genre = jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, genreRowMapper, id);
            return Optional.ofNullable(genre);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Genre> getGenresByFilmId(Long filmId) {
        String sqlQuery = """
                SELECT g.*
                FROM genre AS g
                LEFT JOIN genre_films AS gf ON g.id = gf.genre_id
                LEFT JOIN films AS f ON gf.film_id = f.id
                WHERE f.id = ?""";
        return jdbcTemplate.query(sqlQuery, genreRowMapper, filmId);
    }

    public List<Genre> findAllById(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT * FROM genres WHERE genre_id IN (" + placeholders + ")";

        return jdbcTemplate.query(sql, genreRowMapper, ids.toArray());
    }
}