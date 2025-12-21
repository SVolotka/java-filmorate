package ru.yandex.practicum.filmorate.repositories;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.ReviewRepository;
import ru.yandex.practicum.filmorate.dal.mappers.ReviewRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import({ReviewRepository.class, ReviewRowMapper.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql({"/schema.sql", "/data.sql"})
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ReviewRepositoryTest {

    private final ReviewRepository reviewRepository;

    @Test
    void shouldFindAllReviewsWithLimit() {
        List<Review> reviews = reviewRepository.getAllReviews(10);

        assertThat(reviews).hasSize(4);

        assertThat(reviews.get(0).getReviewId()).isEqualTo(4);
        assertThat(reviews.get(1).getReviewId()).isEqualTo(1);
        assertThat(reviews.get(2).getReviewId()).isEqualTo(2);
        assertThat(reviews.get(3).getReviewId()).isEqualTo(3);
    }

    @Test
    void shouldFindAllReviewsWithSmallLimit() {
        List<Review> reviews = reviewRepository.getAllReviews(2);

        assertThat(reviews).hasSize(2);
        assertThat(reviews.get(0).getReviewId()).isEqualTo(4);
        assertThat(reviews.get(1).getReviewId()).isEqualTo(1);
    }

    @Test
    void shouldGetReviewById() {
        Review review = reviewRepository.getById(1);

        assertThat(review).isNotNull();
        assertThat(review.getReviewId()).isEqualTo(1);
        assertThat(review.getContent()).isEqualTo("Отличный фильм!");
        assertThat(review.getIsPositive()).isTrue();
        assertThat(review.getUserId()).isEqualTo(1);
        assertThat(review.getFilmId()).isEqualTo(1);
        assertThat(review.getUseful()).isEqualTo(10);
    }

    @Test
    void shouldReturnNullWhenReviewNotFound() {
        Review review = reviewRepository.getById(999);

        assertThat(review).isNull();
    }

    @Test
    void shouldAddNewReview() {
        Review newReview = Review.builder()
                .content("Новый тестовый отзыв")
                .isPositive(true)
                .userId(2L)
                .filmId(2L)
                .useful(0)
                .build();

        Review createdReview = reviewRepository.add(newReview);

        assertThat(createdReview).isNotNull();
        assertThat(createdReview.getReviewId()).isPositive();
        assertThat(createdReview.getContent()).isEqualTo("Новый тестовый отзыв");
        assertThat(createdReview.getUserId()).isEqualTo(2);
        assertThat(createdReview.getFilmId()).isEqualTo(2);

        Review savedReview = reviewRepository.getById(createdReview.getReviewId());
        assertThat(savedReview).isNotNull();
        assertThat(savedReview.getContent()).isEqualTo("Новый тестовый отзыв");
    }

    @Test
    void shouldAddReviewWithNullUseful() {
        Review newReview = Review.builder()
                .content("Отзыв без полезности")
                .isPositive(false)
                .userId(2L)
                .filmId(1L)
                .useful(null)
                .build();

        Review createdReview = reviewRepository.add(newReview);
        assertThat(createdReview).isNotNull();
        assertThat(createdReview.getUseful()).isNotNull();
        assertThat(createdReview.getUseful()).isEqualTo(0);
    }

    @Test
    void shouldDeleteReview() {
        int reviewId = 3;
        Review reviewBeforeDelete = reviewRepository.getById(reviewId);
        assertThat(reviewBeforeDelete).isNotNull();

        reviewRepository.delete(reviewId);

        Review reviewAfterDelete = reviewRepository.getById(reviewId);
        assertThat(reviewAfterDelete).isNull();

        List<Review> allReviews = reviewRepository.getAllReviews(10);
        assertThat(allReviews).hasSize(3);
    }

    @Test
    void shouldNotThrowWhenDeletingNonExistentReview() {
        assertDoesNotThrow(() -> reviewRepository.delete(999));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUpdatingNonExistentReview() {
        Review nonExistentReview = Review.builder()
                .reviewId(999L)
                .content("Несуществующий отзыв")
                .isPositive(true)
                .useful(0)
                .build();

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> reviewRepository.update(nonExistentReview));

        assertThat(exception.getMessage()).contains("не найден");
    }

    @Test
    void shouldFindReviewsByFilmId() {
        List<Review> film1Reviews = reviewRepository.findByFilmId(1, 10);
        List<Review> film2Reviews = reviewRepository.findByFilmId(2, 10);

        assertThat(film1Reviews).hasSize(2);
        assertThat(film1Reviews).allMatch(r -> r.getFilmId() == 1);

        assertThat(film2Reviews).hasSize(2);
        assertThat(film2Reviews).allMatch(r -> r.getFilmId() == 2);

        assertThat(film1Reviews.get(0).getReviewId()).isEqualTo(1);
        assertThat(film1Reviews.get(1).getReviewId()).isEqualTo(2);
    }

    @Test
    void shouldFindReviewsByFilmIdWithLimit() {
        List<Review> reviews = reviewRepository.findByFilmId(1, 1);

        assertThat(reviews).hasSize(1);
        assertThat(reviews.getFirst().getFilmId()).isEqualTo(1);
        assertThat(reviews.getFirst().getReviewId()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyListWhenNoReviewsForFilm() {
        List<Review> reviews = reviewRepository.findByFilmId(999, 10);

        assertThat(reviews).isEmpty();
    }

    @Test
    void shouldMaintainOrderByUsefulThenById() {
        Review review5 = Review.builder()
                .content("Еще один отзыв")
                .isPositive(true)
                .userId(2L)
                .filmId(1L)
                .useful(5)
                .build();

        Review createdReview = reviewRepository.add(review5);

        List<Review> filmReviews = reviewRepository.findByFilmId(1, 10);

        assertThat(filmReviews).hasSize(3);
        assertThat(filmReviews.get(0).getReviewId()).isEqualTo(1);
        assertThat(filmReviews.get(1).getReviewId()).isEqualTo(2);
        assertThat(filmReviews.get(2).getReviewId()).isEqualTo(createdReview.getReviewId());
    }
}