package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.UserFeedRowMapper;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.model.UserFeed;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserFeedRepository {

    private static final String INSERT_QUERY = """
                    INSERT INTO user_events (user_id, event_type, operation, entity_id, timestamp)
                    VALUES (?, ?, ?, ?, ?)
                    """;

    private static final String GET_BY_USER_ID_QUERY = """
            SELECT event_id, user_id, event_type, operation, entity_id, timestamp
            FROM user_events WHERE user_id = ? ORDER BY timestamp DESC
            """;

    private static final String DELETE_LIKE_EVENTS_BY_FILM_ID_QUERY =
            "DELETE FROM user_events WHERE event_type = 'LIKE' AND entity_id = ?";

    private static final String DELETE_REVIEW_EVENTS_BY_REVIEW_ID_QUERY =
            "DELETE FROM user_events WHERE event_type = 'REVIEW' AND entity_id = ?";

    private static final String DELETE_FRIEND_EVENTS_BY_FRIEND_ID_QUERY =
            "DELETE FROM user_events WHERE event_type = 'FRIEND' AND entity_id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final UserFeedRowMapper userFeedRowMapper;

    public UserFeed create(UserFeed userFeed) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        int rowsAffected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userFeed.getUserId());
            ps.setString(2, userFeed.getEventType().name());
            ps.setString(3, userFeed.getOperation().name());
            ps.setLong(4, userFeed.getEntityId());
            ps.setLong(5, userFeed.getTimestamp());
            return ps;
        }, keyHolder);

        if (rowsAffected == 0) {
            throw new InternalServerException("Failed to create user feed event");
        }

        Long eventId = keyHolder.getKeyAs(Long.class);

        if(eventId == null) {
            throw new InternalServerException("Generated key is null");
        }

        userFeed.setEventId(eventId);
        return userFeed;
    }

    public List<UserFeed> getByUserId(Long userId) {
        return jdbcTemplate.query(GET_BY_USER_ID_QUERY,userFeedRowMapper, userId);
    }

    public void deleteLikeEventsByFilmId(Long filmId) {
        jdbcTemplate.update(DELETE_LIKE_EVENTS_BY_FILM_ID_QUERY, filmId);
    }

    public void deleteFriendEventsByFriendId(Long friendId) {
        jdbcTemplate.update(DELETE_FRIEND_EVENTS_BY_FRIEND_ID_QUERY, friendId);
    }

    public void deleteReviewEventsByReviewId(Long reviewId) {
        jdbcTemplate.update(DELETE_REVIEW_EVENTS_BY_REVIEW_ID_QUERY, reviewId);
    }
}