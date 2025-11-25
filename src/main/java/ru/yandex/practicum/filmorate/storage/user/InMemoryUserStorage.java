package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.UserAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final HashMap<Long, User> users = new HashMap<>();

    @Override
    public void create(User user) {
        if (users.containsKey(user.getId())) {
            String errorMessage = String.format("User with id %d already exist", user.getId());
            throw new UserAlreadyExistException(errorMessage);
        }
        users.put(user.getId(), user);
    }

    @Override
    public void update(User user) {
        if (!users.containsKey(user.getId())) {
            String errorMessage = String.format("User with id %d not found", user.getId());
            throw new UserNotFoundException(errorMessage);
        }
        User existingUser = users.get(user.getId());

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getLogin() != null && !user.getLogin().isBlank()) {
            existingUser.setLogin(user.getLogin());
        }
        if (user.getName() != null) {
            existingUser.setName(user.getName());
        }
        if (user.getBirthday() != null) {
            existingUser.setBirthday(user.getBirthday());
        }
        if (user.getFriends() != null) {
            existingUser.setFriends(user.getFriends());
        }
        users.put(user.getId(), existingUser);
    }

    @Override
    public User get(long id) {
        if (!users.containsKey(id)) {
            String errorMessage = String.format("User with id %d not found", id);
            throw new UserNotFoundException(errorMessage);
        }
        return users.get(id);
    }

    @Override
    public List<User> getAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void addFriend(long firstId, long secondId) {
        if (firstId == secondId) {
            throw new ValidationException("User cannot add themselves as friend");
        }
        User firstUser = get(firstId);
        User secondUser = get(secondId);
        firstUser.getFriends().add(secondId);
        secondUser.getFriends().add(firstId);
    }

    @Override
    public void removeFriend(long firstId, long secondId) {
        User firstUser = get(firstId);
        User secondUser = get(secondId);

        if (firstUser.getFriends() == null) {
            firstUser.setFriends(new HashSet<>());
        }
        if (secondUser.getFriends() == null) {
            secondUser.setFriends(new HashSet<>());
        }

        firstUser.getFriends().remove(secondId);
        secondUser.getFriends().remove(firstId);
    }

    @Override
    public List<User> getFriendsById(long id) {
        User user = get(id);
        Set<Long> friendIds = user.getFriends();
        if (friendIds == null) {
            return new ArrayList<>();
        }

        List<User> friends = new ArrayList<>();
        for (Long friendId : friendIds) {
            friends.add(get(friendId));
        }
        return friends;
    }

    @Override
    public List<User> getCommonFriends(long firstId, long secondId) {
        User firstUser = get(firstId);
        User secondUser = get(secondId);

        Set<Long> firstUserFriends = firstUser.getFriends();
        Set<Long> secondUserFriends = secondUser.getFriends();

        Set<Long> commonFriendIds = new HashSet<>(firstUserFriends);
        commonFriendIds.retainAll(secondUserFriends);

        List<User> commonFriends = new ArrayList<>();
        for (Long friendId : commonFriendIds) {
            commonFriends.add(get(friendId));
        }
        return commonFriends;
    }
}