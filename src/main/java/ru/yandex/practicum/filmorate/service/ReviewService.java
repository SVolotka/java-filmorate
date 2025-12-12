package ru.yandex.practicum.filmorate.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.ReviewRepository;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

@Service
@AllArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

//    public Review getReviewById(Integer id) {
//
//        return reviewRepository.getReviewById(id);
//    }
//
    public Review add(Review newReview) {
        return reviewRepository.add(newReview);
    }
//
//    public Review update(Review updatedReview) {
//        return reviewStorage.update(updatedReview);
//    }

    public List<Review> getAllReviews() {
        return reviewRepository.getAllReviews();
    }

    public void delete(int reviewId) {
        reviewRepository.delete(reviewId);
    }
}
