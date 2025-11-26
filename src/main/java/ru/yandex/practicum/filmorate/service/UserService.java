package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private long counter = 0L;

    private final UserStorage userStorage;
    private final FilmStorage filmStorage;

    public User create(User user) {
        validateUser(user);
        checkAndSetName(user);
        user.setId(++counter);
        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }
        userStorage.create(user);
        return user;
    }

    public User get(long id) {
        return userStorage.get(id);
    }

    public List<User> findAll() {
        return userStorage.getAll();
    }

    public User update(User user) {
        userStorage.get(user.getId());
        validateUser(user);
        checkAndSetName(user);
        userStorage.update(user);
        return userStorage.get(user.getId());
    }

    public void addFriend(long firstId, long secondId) {
        if (firstId == secondId) {
            throw new ValidationException("User cannot add themselves as friend");
        }
        userStorage.addFriend(firstId, secondId);
    }

    public void removeFriend(long firstId, long secondId) {
        if (firstId == secondId) {
            throw new ValidationException("User cannot remove themselves as friend");
        }
        userStorage.removeFriend(firstId, secondId);
    }

    public List<User> getFriendsById(long id) {
        return userStorage.getFriendsById(id);
    }

    public List<User> getCommonFriends(long firstId, long secondId) {
        return userStorage.getCommonFriends(firstId, secondId);
    }

    private void checkAndSetName(User user) {
        String name = user.getName();
        if (name == null || name.isBlank()) {
            user.setName(user.getLogin());
        }
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Email cannot be null or blank");
        }
        if (!user.getEmail().contains("@")) {
            throw new ValidationException("Email must contain @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Login cannot be null or blank");
        }
        if (user.getLogin().contains(" ")) {
            throw new ValidationException("Login cannot contain spaces");
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(java.time.LocalDate.now())) {
            throw new ValidationException("Birthday cannot be in the future");
        }
    }
}