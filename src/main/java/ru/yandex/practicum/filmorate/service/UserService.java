package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.UserFeedRepository;
import ru.yandex.practicum.filmorate.dal.UserRepository;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.UserFeed;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserFeedRepository userFeedRepository;

    public User create(User user) {
        validateUser(user);
        checkAndSetName(user);
        return userRepository.create(user);
    }

    public User get(long id) {
        if (!userRepository.exists(id)) {
            throw new UserNotFoundException("User with id=" + id + " not found");
        }
        return userRepository.get(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User update(User user) {
        validateUser(user);
        checkAndSetName(user);
        if (!userRepository.exists(user.getId())) {
            throw new UserNotFoundException("User with id=" + user.getId() + " not found");
        }
        return userRepository.update(user);
    }

    @Transactional
    public void addFriend(long userId, long friendId) {
        if (!userRepository.exists(userId)) {
            throw new UserNotFoundException("User with id=" + userId + " not found");
        }
        if (!userRepository.exists(friendId)) {
            throw new UserNotFoundException("User with id=" + friendId + " not found");
        }
        userRepository.addFriend(userId, friendId);
        logFriendEvent(userId, friendId, Operation.ADD);
    }

   @Transactional
    public void removeFriend(long userId, long friendId) {
        if (!userRepository.exists(userId)) {
            throw new UserNotFoundException("User with id=" + userId + " not found");
        }
        if (!userRepository.exists(friendId)) {
            throw new UserNotFoundException("User with id=" + friendId + " not found");
        }
        userRepository.removeFriend(userId, friendId);
        logFriendEvent(userId, friendId, Operation.REMOVE);
    }

    public List<User> getFriendsById(long id) {
        if (!userRepository.exists(id)) {
            throw new UserNotFoundException("User with id=" + id + " not found");
        }
        return userRepository.getFriends(id);
    }

    public List<User> getCommonFriends(long userId, long friendId) {
        if (!userRepository.exists(userId)) {
            throw new UserNotFoundException("User with id=" + userId + " not found");
        }
        if (!userRepository.exists(friendId)) {
            throw new UserNotFoundException("User with id=" + friendId + " not found");
        }
        return userRepository.getCommonFriends(userId, friendId);
    }

    public List<UserFeed> getUserFeed(long userId) {
        if (!userRepository.exists(userId)) {
            throw new UserNotFoundException("User with id=" + userId + " not found");
        }
        return userFeedRepository.getByUserId(userId);
    }

    private void checkAndSetName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
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
        if (user.getLogin().chars().anyMatch(Character::isWhitespace)) {
            throw new ValidationException("Login cannot contain whitespace characters");
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Birthday cannot be in the future");
        }
    }

    private void logFriendEvent(long userId, long friendId, Operation operation) {
        UserFeed userFeed = new UserFeed();
        userFeed.setUserId(userId);
        userFeed.setEntityId(friendId);
        userFeed.setEventType(EventType.FRIEND);
        userFeed.setOperation(operation);
        userFeed.setTimestamp(System.currentTimeMillis());
        userFeedRepository.create(userFeed);
    }
}