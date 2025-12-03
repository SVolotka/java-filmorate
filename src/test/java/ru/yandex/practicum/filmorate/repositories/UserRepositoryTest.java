package ru.yandex.practicum.filmorate.repositories;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.UserRepository;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({UserRepository.class, UserRowMapper.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql({"/schema.sql", "/data.sql"})
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserRepositoryTest {

    private final UserRepository userRepository;

    @Test
    void shouldFindAllUsers() {
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(2);
    }

    @Test
    void shouldGetUserById() {
        User user = userRepository.get(1L);
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getLogin()).isEqualTo("user1");
        assertThat(user.getEmail()).isEqualTo("user1@test.com");
    }

    @Test
    void shouldAddFriend() {
        List<User> friendsOf1 = userRepository.getFriends(1L);
        assertThat(friendsOf1).isEmpty();

        List<User> friendsOf2 = userRepository.getFriends(2L);
        assertThat(friendsOf2).isEmpty();

        userRepository.addFriend(1L, 2L);

        friendsOf1 = userRepository.getFriends(1L);
        assertThat(friendsOf1).hasSize(1);
        assertThat(friendsOf1.getFirst().getId()).isEqualTo(2L);

        friendsOf2 = userRepository.getFriends(2L);
        assertThat(friendsOf2).isEmpty();
    }

    @Test
    void shouldRemoveFriend() {
        userRepository.addFriend(1L, 2L);

        List<User> friends = userRepository.getFriends(1L);
        assertThat(friends).hasSize(1);

        userRepository.removeFriend(1L, 2L);

        friends = userRepository.getFriends(1L);
        assertThat(friends).isEmpty();
    }
}