package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.ReviewRowMapper;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Repository
public class ReviewRepository {
    private static final String FIND_ALL_QUERY = "SELECT * FROM reviews";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM reviews r" +
            "WHERE r.review_id = ?";
    private static final String CREATE_QUERY = "INSERT INTO reviews " +
            "(content, is_positive, user_id, film_id, useful) VALUES (?, ?, ?, ?, ?)";
    private static final String DELETE_QUERY = "DELETE FROM reviews WHERE review_id = ?";

    private final JdbcTemplate jdbc;
    private final ReviewRowMapper reviewRowMapper;

    public ReviewRepository(JdbcTemplate jdbc, ReviewRowMapper reviewRowMapper) {
        this.jdbc = jdbc;
        this.reviewRowMapper = reviewRowMapper;
    }

    public List<Review> getAllReviews() {
        List<Review> reviews = jdbc.query(FIND_ALL_QUERY, reviewRowMapper);

        return reviews;
    }

    public Review getById(int id) {
        Review review = jdbc.queryForObject(FIND_BY_ID_QUERY, reviewRowMapper, id);

        return review;
    }

    public Review add(Review review) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        System.out.println(CREATE_QUERY == null ? "NULL" : CREATE_QUERY.length());
        int affectRows = jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(CREATE_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setInt(3, review.getUserId());
            ps.setInt(4, review.getFilmId());
            ps.setInt(5, review.getUseful());
            return ps;
        }, keyHolder);

        if (affectRows == 0) {
            throw new RuntimeException("Не удалось добавить отзыв");
        }

        int reviewId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        review.setReviewId(reviewId);

        return  review;
    }

    public void delete(int reviewId) {
        jdbc.update(DELETE_QUERY, reviewId);
    }
}
