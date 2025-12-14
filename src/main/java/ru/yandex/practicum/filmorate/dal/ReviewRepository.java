package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.ReviewRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReviewRepository {
    private static final String FIND_ALL_LIMIT_QUERY = "SELECT * FROM reviews ORDER BY useful DESC, review_id LIMIT ?";
    private static final String FIND_BY_FILM_ID_LIMIT_QUERY = "SELECT * FROM reviews WHERE film_id = ? " +
            "ORDER BY useful DESC, review_id LIMIT ?";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM reviews r " +
            "WHERE r.review_id = ?";
    private static final String CREATE_QUERY = "INSERT INTO reviews " +
            "(content, is_positive, user_id, film_id, useful) VALUES (?, ?, ?, ?, ?)";
    private static final String DELETE_QUERY = "DELETE FROM reviews WHERE review_id = ?";
    private static final String UPDATE_QUERY = "UPDATE reviews SET content = ?, is_positive = ?, useful = ? WHERE review_id = ?";

    private final JdbcTemplate jdbc;
    private final ReviewRowMapper reviewRowMapper;

    public List<Review> getAllReviews(int count) {
        log.debug("Получение всех отзывов с лимитом {}", count);
        List<Review> reviews = jdbc.query(FIND_ALL_LIMIT_QUERY, reviewRowMapper, count);

        log.debug("Найден {} отзывов", reviews.size());
        return reviews;
    }

    public Review getById(int id) {
        log.debug("Поиск отзыва по id: {}", id);
        try {
            Review review = jdbc.queryForObject(FIND_BY_ID_QUERY, reviewRowMapper, id);
            log.debug("Отзыв с id {} найден", id);
            return review;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Отзыв с id {} не найден", id);
            return null;
        }
    }

    public Review add(Review review) {
        log.info("Добавление нового отзыва от пользователя {} для фильма {}",
                review.getUserId(), review.getFilmId());

        log.debug("Данные отзыва: content='{}', isPositive={}",
                review.getContent(), review.getIsPositive());

        KeyHolder keyHolder = new GeneratedKeyHolder();

        if (review.getUseful() == null) {
            review.setUseful(0);
        }

        int affectRows = jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(CREATE_QUERY, new String[]{"review_id"});
            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setInt(3, review.getUserId());
            ps.setInt(4, review.getFilmId());
            ps.setInt(5, review.getUseful() != null ? review.getUseful() : 0);
            return ps;
        }, keyHolder);

        if (affectRows == 0) {
            throw new RuntimeException("Не удалось добавить отзыв");
        }

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new RuntimeException("Сгенерированный id не был возвращен");
        }

        review.setReviewId(key.intValue());

        log.debug("Отзыв добавлен с id: {}", key.intValue());
        return review;
    }

    public void delete(int reviewId) {
        log.info("Удаление отзыва с id: {}", reviewId);
        int deleteRows = jdbc.update(DELETE_QUERY, reviewId);

        if (deleteRows == 0) {
            log.warn("Попытка удалить несуществующий отзыв с id: {}", reviewId);
        } else {
            log.info("Отзыв с id: {} успешно удален. Удалено строк: {}", reviewId, deleteRows);
        }
    }

    public Review update(Review review) {
        log.info("Обновление отзыва с id: {}", review.getReviewId());
        log.debug("Новые данные отзыва: content='{}', isPositive={}, useful={}",
                review.getContent(), review.getIsPositive(),
                review.getUseful() != null ? review.getUseful() : 0);

        int update = jdbc.update(UPDATE_QUERY,
                review.getContent(),
                review.getIsPositive(),
                review.getUseful() != null ? review.getUseful() : 0,
                review.getReviewId()
                );

        if (update == 0) {
            log.error("Отзыв с id {} не найден для обновления", review.getReviewId());
            throw new NotFoundException(String.format("Отзыв с id %s не найден", review.getReviewId()));
        }

        log.debug("Отзыв с id: {} обновлен, обновлено строк: {}", review.getReviewId(), update);

        Review updatedReview = getById(review.getReviewId());

        if (updatedReview == null) {
            log.error("Отзыв с id: {} не найден после обновления", review.getReviewId());
        }

        log.info("Отзыв с id: {} успешно обновлен", review.getReviewId());

        return updatedReview;
    }

    public List<Review> findByFilmId(int filmId, int count) {
        log.debug("Поиск отзывов для фильма с id: {} с лимитом {}", filmId, count);

        List<Review> reviews = jdbc.query(FIND_BY_FILM_ID_LIMIT_QUERY, reviewRowMapper, filmId, count);

        log.debug("Найдено {} отзывов для фильма с id: {}", reviews.size(), filmId);

        return reviews;
    }
}
