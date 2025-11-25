package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface UserStorage {
    void create(User user);

    void update(User user);

    User get(long id);

    List<User> getAll();

    void addFriend(long firstId, long secondId);

    void removeFriend(long firstId, long secondId);

    List<User> getFriendsById(long id);

    List<User> getCommonFriends(long firstId, long secondId);
}