package ru.yandex.practicum.filmorate.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.ReviewLikeRepository;
import ru.yandex.practicum.filmorate.dal.ReviewRepository;
import ru.yandex.practicum.filmorate.dal.UserFeedRepository;
import ru.yandex.practicum.filmorate.dal.UserRepository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.UserFeed;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;
    private final UserFeedRepository userFeedRepository;

    public Review getById(long id) {
        checkExistsReview(id);
        return reviewRepository.getById(id);
    }

    @Transactional
    public Review add(Review newReview) {
        checkCorrectReview(newReview);
        Review savedReview = reviewRepository.add(newReview);
        logReviewEvent(savedReview.getUserId(), savedReview.getReviewId(), Operation.ADD);
        return savedReview;
    }

    @Transactional
    public Review update(Review updatedReview) {
        checkCorrectReview(updatedReview);
        Review savedUpdatedReview = reviewRepository.update(updatedReview);
        logReviewEvent(savedUpdatedReview.getUserId(), savedUpdatedReview.getReviewId(), Operation.UPDATE);
        return savedUpdatedReview;
    }

    public List<Review> getAllReviews(Long filmId, int count) {
        List<Review> reviews;
        if (filmId == null) {
            reviews = reviewRepository.getAllReviews(count);
        } else {
            reviews = reviewRepository.findByFilmId(filmId, count);
        }
        return reviews;
    }

    @Transactional
    public void delete(long reviewId) {
        Review review = getById(reviewId);
        reviewRepository.delete(reviewId);
        logReviewEvent(review.getUserId(), review.getReviewId(), Operation.REMOVE);
    }

    public void addLike(long reviewId, Long userId, boolean isLike) {
        reviewLikeRepository.addLike(reviewId, userId, isLike);
    }

    public void addDislike(long reviewId, Long userId) {
        reviewLikeRepository.addDislike(reviewId, userId);
    }

    public void deleteLike(long reviewId, Long userId) {
        reviewLikeRepository.deleteLike(reviewId, userId);
    }

    public void deleteDislike(long reviewId, Long userId) {
        reviewLikeRepository.deleteDislike(reviewId, userId);
    }

    private void checkExistsReview(long reviewId) {
        Review review = reviewRepository.getById(reviewId);
        if (review == null) {
            log.warn("Отзыв с id {} не найден", reviewId);
            throw new NotFoundException(String.format("Отзыв с id %s не найден", reviewId));
        }
    }

    private void checkCorrectReview(Review review) {
        if (review.getContent() == null || review.getContent().isBlank()) {
            throw new ValidationException("Текст отзыва заполнен некорректно");
        }

        if (review.getUserId() == null || userRepository.get(review.getUserId()) == null) {
            throw new ValidationException("Данные о пользователе заполнены некорректно");
        }
        if (review.getFilmId() == null) {
            throw new ValidationException("Данные о фильме заполнены некорректно");
        }

        if (review.getFilmId() < 1) {
            throw new NotFoundException(String.format("Фильм с id %s не существует", review.getFilmId()));
        }

        if (review.getIsPositive() == null) {
            throw new ValidationException("Некорректно заполнены данные о типе отзыва");
        }

    }

    private void logReviewEvent(long userId, long reviewId, Operation operation) {
        UserFeed userFeed = new UserFeed();

        userFeed.setUserId(userId);
        userFeed.setEntityId(reviewId);
        userFeed.setEventType(EventType.REVIEW);
        userFeed.setOperation(operation);
        userFeed.setTimestamp(System.currentTimeMillis());

        userFeedRepository.create(userFeed);
    }
}