package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Collection<User> findAll() {
        log.info("Получен HTTP-запрос на получение всех пользователей");
        List<User> allUsers = userService.findAll();
        log.info("Успешно обработан HTTP-запрос на получение всех пользователей");
        return allUsers;
    }

    @PostMapping
    public User create(@RequestBody @Valid User user) {
        log.info("Получен HTTP-запрос на создание пользователя: {}", user);
        userService.create(user);
        log.info("Успешно обработан HTTP-запрос на создание пользователя: {}", user);
        return user;
    }

    @PutMapping
    public User update(@RequestBody @Valid User user) {
        log.info("Получен HTTP-запрос на обновление пользователя: {}", user);
        User updatedUser = userService.update(user);
        log.info("Успешно обработан HTTP-запрос на обновление пользователя: {}", updatedUser);
        return updatedUser;
    }

    @GetMapping("/{id}")
    public User get(@PathVariable long id) {
        log.info("Получен HTTP-запрос на получение пользователя с id: {}", id);
        User existingUser = userService.get(id);
        log.info("Успешно обработан HTTP-запрос на получение пользователя с id: {}", id);
        return existingUser;
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable long id, @PathVariable long friendId) {
        log.info("Получен HTTP-запрос на добавление пользователей с id: {} и {} в друзья", id, friendId);
        userService.addFriend(id, friendId);
        log.info("Успешно обработан HTTP-запрос на добавление пользователей с id: {} и {} в друзья", id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void deleteFriend(@PathVariable long id, @PathVariable long friendId) {
        log.info("Получен HTTP-запрос на удаление пользователей с id: {} и {} из друзей", id, friendId);
        userService.removeFriend(id, friendId);
        log.info("Успешно обработан HTTP-запрос на удаление пользователей с id: {} и {} из друзей", id, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriendById(@PathVariable long id) {
        log.info("Получен HTTP-запрос на получение списка друзей пользователя с id: {}", id);
        List<User> friends = userService.getFriendsById(id);
        log.info("Успешно обработан HTTP-запрос на получение списка друзей пользователя с id: {}", id);
        return friends;
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable long id, @PathVariable long otherId) {
        log.info("Получен HTTP-запрос на получение общих друзей пользователей с id: {} и {}", id, otherId);
        List<User> commonFriends = userService.getCommonFriends(id, otherId);
        log.info("Успешно обработан HTTP-запрос на получение общих друзей пользователей с id: {} и {}", id, otherId);
        return commonFriends;
    }
}