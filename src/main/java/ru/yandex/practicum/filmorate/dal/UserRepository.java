package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private static final String FIND_ALL_QUERY = "SELECT user_id, email, login, name, birthday FROM users";
    private static final String FIND_BY_ID_QUERY = "SELECT user_id, email, login, name, birthday FROM users WHERE user_id = ?";
    private static final String INSERT_QUERY = "INSERT INTO users (login, name, email, birthday) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_QUERY = "UPDATE users SET login = ?, name = ?, email = ?, birthday = ? WHERE user_id = ?";

    private static final String ADD_FRIEND_QUERY = """
        INSERT INTO friends (user_id, friend_id)
        SELECT ?, ? WHERE NOT EXISTS (
            SELECT 1 FROM friends WHERE user_id = ? AND friend_id = ?
        )
        """;

    private static final String REMOVE_FRIEND_QUERY = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";

    private static final String GET_FRIENDS_QUERY = """
        SELECT u.user_id, u.email, u.login, u.name, u.birthday
        FROM users u
        INNER JOIN friends f ON u.user_id = f.friend_id
        WHERE f.user_id = ?
        """;

    private static final String GET_COMMON_FRIENDS_QUERY = """
        SELECT u.user_id, u.email, u.login, u.name, u.birthday
        FROM users u
        INNER JOIN friends f1 ON u.user_id = f1.friend_id
        INNER JOIN friends f2 ON u.user_id = f2.friend_id
        WHERE f1.user_id = ? AND f2.user_id = ?
        """;

    private static final String EXISTS_USER_QUERY = "SELECT 1 FROM users WHERE user_id = ?";
    private static final String IS_FRIEND_QUERY = "SELECT 1 FROM friends WHERE user_id = ? AND friend_id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    public List<User> findAll() {
        return jdbcTemplate.query(FIND_ALL_QUERY, userRowMapper);
    }

    public User create(User user) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int rowsAffected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getLogin());
            ps.setString(2, user.getName());
            ps.setString(3, user.getEmail());
            ps.setTimestamp(4, user.getBirthday() != null
                    ? Timestamp.valueOf(user.getBirthday().atStartOfDay())
                    : null);
            return ps;
        }, keyHolder);

        if (rowsAffected == 0) {
            throw new InternalServerException("Failed to create user");
        }

        Long id = keyHolder.getKeyAs(Long.class);
        if (id == null) {
            throw new InternalServerException("Generated key is null");
        }
        user.setId(id);
        return user;
    }

    public User get(long id) {
        try {
            return jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, userRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("User with id=" + id + " not found");
        }
    }

    public User update(User user) {
        int rowsUpdated = jdbcTemplate.update(UPDATE_QUERY,
                user.getLogin(),
                user.getName(),
                user.getEmail(),
                user.getBirthday() != null
                        ? Timestamp.valueOf(user.getBirthday().atStartOfDay())
                        : null,
                user.getId()
        );

        if (rowsUpdated == 0) {
            throw new NotFoundException("User with id=" + user.getId() + " not found");
        }
        return user;
    }

    public boolean exists(long id) {
        try {
            jdbcTemplate.queryForObject(EXISTS_USER_QUERY, Integer.class, id);
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    public void addFriend(long userId, long friendId) {
        if (userId == friendId) {
            throw new ValidationException("User cannot add themselves as friend");
        }
        if (!exists(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
        if (!exists(friendId)) {
            throw new NotFoundException("User with id=" + friendId + " not found");
        }

        if (isFriend(userId, friendId)) {
            throw new ValidationException("Users " + userId + " and " + friendId + " are already friends");
        }

        jdbcTemplate.update(ADD_FRIEND_QUERY, userId, friendId, userId, friendId);
        jdbcTemplate.update(ADD_FRIEND_QUERY, friendId, userId, friendId, userId);
    }

    public void removeFriend(long userId, long friendId) {
        if (!exists(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
        if (!exists(friendId)) {
            throw new NotFoundException("User with id=" + friendId + " not found");
        }

        jdbcTemplate.update(REMOVE_FRIEND_QUERY, userId, friendId);
        jdbcTemplate.update(REMOVE_FRIEND_QUERY, friendId, userId);
    }

    public List<User> getFriends(long userId) {
        if (!exists(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
        return jdbcTemplate.query(GET_FRIENDS_QUERY, userRowMapper, userId);
    }

    public List<User> getCommonFriends(long userId1, long userId2) {
        if (!exists(userId1)) {
            throw new NotFoundException("User with id=" + userId1 + " not found");
        }
        if (!exists(userId2)) {
            throw new NotFoundException("User with id=" + userId2 + " not found");
        }
        return jdbcTemplate.query(GET_COMMON_FRIENDS_QUERY, userRowMapper, userId1, userId2);
    }

    private boolean isFriend(long userId, long friendId) {
        try {
            jdbcTemplate.queryForObject(IS_FRIEND_QUERY, Integer.class, userId, friendId);
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
}