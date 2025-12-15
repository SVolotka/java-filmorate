package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DirectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public Map<Long, Director> getAllDirectors() {
        String sqlQuery = """
                SELECT *
                FROM directors""";

        List<Director> directors = jdbcTemplate.query(sqlQuery, this::rowMapper);

        return directors.stream()
                .collect(Collectors.toMap(Director::getId, Function.identity()));
    }

    public Director getDirectorById(Long directorId) {
        String sqlQuery = """
                SELECT *
                FROM directors
                WHERE id = ?""";

        try {
            return jdbcTemplate.queryForObject(sqlQuery, new Object[] {directorId}, this::rowMapper);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Режиссер с id = " + directorId + " отсутствует в БД");
        }
    }

    public List<Director> getDirectorsByFilmId(Long filmId) {
        String sqlQuery = """
                SELECT d.*
                FROM directors d
                LEFT JOIN directors_films df ON d.id = df.director_id
                LEFT JOIN films f ON df.film_id = f.film_id
                WHERE f.film_id = ?""";
        return jdbcTemplate.query(sqlQuery, this::rowMapper, filmId);
    }

    public Director createDirector(Director director) {
        String sqlQuery = """
                INSERT INTO directors(name)
                VALUES(?)""";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, director.getName());
            return ps;
        }, keyHolder);
        director.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return director;
    }

    public Director updateDirector(Director director) {

        try {
            getDirectorById(director.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Режиссер с id = " + director.getId() + " отсутствует в БД");
        }

        String sqlQuery = """
                UPDATE directors
                SET name = ?
                WHERE id = ?""";

        jdbcTemplate.update(sqlQuery, director.getName(), director.getId());
        return director;
    }

    public void deleteDirector(Long directorId) {
        String sqlQuery = """
                DELETE FROM directors
                WHERE id = ?""";

        jdbcTemplate.update(sqlQuery, directorId);
    }

    private Director rowMapper(ResultSet resultSet, int rowNum) throws SQLException {
        return Director.builder()
                .id(resultSet.getLong("id"))
                .name(resultSet.getString("name"))
                .build();
    }
}
