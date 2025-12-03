package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.MpaRowMapper;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MpaRepository {
    private static final String FIND_ALL_QUERY = "SELECT rating_id, name FROM mpa_rating ORDER BY rating_id";
    private static final String FIND_BY_ID_QUERY = "SELECT rating_id, name FROM mpa_rating WHERE rating_id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final MpaRowMapper mpaRowMapper;

    public List<Mpa> findAll() {
        return jdbcTemplate.query(FIND_ALL_QUERY, mpaRowMapper);
    }

    public Optional<Mpa> findById(int id) {
        try {
            Mpa mpa = jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, mpaRowMapper, id);
            return Optional.ofNullable(mpa);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}