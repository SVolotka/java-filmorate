package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReviewLikeRepository {
    private static final String CHECK_LIKE_QUERY = "SELECT is_like FROM review_likes WHERE review_id = ? " +
            "AND user_id = ?";
    private static final String INSERT_LIKE_QUERY = "INSERT INTO review_likes (review_id, user_id, is_like) " +
            "VALUES (?, ?, ?)";
    private static final String UPDATE_LIKE_QUERY = "UPDATE review_likes SET is_like = ? WHERE " +
            "review_id = ? AND user_id = ?";
    private static final String DELETE_LIKE_QUERY = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ?";
    private static final String UPDATE_USEFUL_QUERY = "UPDATE reviews SET useful = COALESCE(useful, 0) + ? " +
            "WHERE review_id = ?";
    private final JdbcTemplate jdbc;

    public void addLike(long reviewId, long userId, boolean isLike) {
        log.info("Добавление {} для отзыва {} пользователем {}", isLike ? "лайка" : "дизлайка", reviewId, userId);
        Boolean existingLike = null;
        try {
            existingLike = jdbc.queryForObject(CHECK_LIKE_QUERY, Boolean.class, reviewId, userId);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Пользователь {} ещё не поставил оценку за отзыв {}", userId, reviewId);
        }

        if (existingLike == null) {
            log.debug("Создание нового голоса для отзыва {} пользователем {}", reviewId, userId);
            jdbc.update(INSERT_LIKE_QUERY, reviewId, userId, isLike);

            int usefulChange = isLike ? 1 : -1;
            updateReviewUseful(reviewId, usefulChange);
        } else if (existingLike != isLike) {
            log.debug("Изменение голоса с {} на {} для отзыва {} пользователем {}",
                    existingLike ? "лайк" : "дизлайк",
                    isLike ? "лайк" : "дизлайк",
                    reviewId, userId);
            int usefulChange = isLike ? 2 : -2;

            jdbc.update(UPDATE_LIKE_QUERY, isLike, reviewId, userId);

            updateReviewUseful(reviewId, usefulChange);
        } else {
            log.debug("Удаление голоса для отзыва {} пользователем {}", reviewId, userId);
            jdbc.update(DELETE_LIKE_QUERY, reviewId, userId);

            int usefulChange = isLike ? -1 : 1;
            updateReviewUseful(reviewId, usefulChange);
        }
        log.info("Голосование для отзыва {} пользователем {} успешно обработано", reviewId, userId);
    }

    public void addDislike(long reviewId, long userId) {
        log.info("Добавление дизлайка для отзыва {} пользователем {}", reviewId, userId);
        addLike(reviewId, userId, false);
    }

    public void deleteLike(long reviewId, long userId) {
        log.info("Удаление голоса для отзыва {} пользователем {}", reviewId, userId);
        try {
            Boolean isLike = jdbc.queryForObject(CHECK_LIKE_QUERY, Boolean.class, reviewId, userId);
            jdbc.update(DELETE_LIKE_QUERY, reviewId, userId);
            int usefulChange = isLike ? -1 : 1;
            updateReviewUseful(reviewId, usefulChange);
            log.debug("Удален {} для отзыва {}",
                    isLike ? "лайк" : "дизлайк", reviewId);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Попытка удалить несуществующий голос для отзыва {} пользователем {}", reviewId, userId);
            throw new NotFoundException("Голос не найден");
        }
    }

    public void deleteDislike(long reviewId, long userId) {
        log.info("Удаление дизлайка для отзыва {} пользователем {}", reviewId, userId);
        deleteLike(reviewId, userId);
    }

    public void updateReviewUseful(long reviewId, int useful) {
        log.debug("Обновление полезности отзыва {} на значение {}", reviewId, useful);
        int changedUseful = jdbc.update(UPDATE_USEFUL_QUERY, useful, reviewId);
        log.debug("Полезность отзыва {} изменена на {}", reviewId, changedUseful);
    }
}