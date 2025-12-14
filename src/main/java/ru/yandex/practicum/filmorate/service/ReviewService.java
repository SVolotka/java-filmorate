package ru.yandex.practicum.filmorate.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.ReviewLikeRepository;
import ru.yandex.practicum.filmorate.dal.ReviewRepository;
import ru.yandex.practicum.filmorate.dal.UserRepository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;

    public Review getById(Integer id) {
        log.error("Отзыв с id {} не найде", id);
        checkExistsReview(id);
        return reviewRepository.getById(id);
    }

    public Review add(Review newReview) {
        checkCorrectReview(newReview);
        return reviewRepository.add(newReview);
    }

    public Review update(Review updatedReview) {
        checkCorrectReview(updatedReview);
        return reviewRepository.update(updatedReview);
    }

    public List<Review> getAllReviews(Integer filmId, int count) {
        List<Review> reviews;
        if (filmId == null) {
            reviews = reviewRepository.getAllReviews(count);
        } else {
            reviews = reviewRepository.findByFilmId(filmId, count);
        }
        return reviews;
    }

    public void delete(int reviewId) {
        reviewRepository.delete(reviewId);
    }

    public void addLike(int reviewId, int userId, boolean isLike) {
        reviewLikeRepository.addLike(reviewId, userId, isLike);
    }

    public void addDislike(int reviewId, int userId) {
        reviewLikeRepository.addDislike(reviewId, userId);
    }

    public void deleteLike(int reviewId, int userId) {
        reviewLikeRepository.deleteLike(reviewId, userId);
    }

    public void deleteDislike(int reviewId, int userId) {
        reviewLikeRepository.deleteDislike(reviewId, userId);
    }

    private void checkExistsReview(int reviewId) {
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
}
