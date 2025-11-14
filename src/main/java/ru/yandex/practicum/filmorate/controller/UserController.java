package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> findAll() {
        log.info("Получен HTTP-запрос на получение всех пользователей");

        return new ArrayList<>(users.values());
    }

    @PostMapping
    public User create(@RequestBody @Valid User user) {
        log.info("Получен HTTP-запрос на создание пользователя: {}", user);

        user.setId(getNextId());
        checkAndSetName(user);
        users.put(user.getId(), user);

        log.info("Успешно обработан HTTP-запрос на создание пользователя: {}", user);
        return user;
    }

    @PutMapping
    public User update(@RequestBody @Valid User newUser) {
        log.info("Получен HTTP-запрос на обновление пользователя: {}", newUser);

        Long id = newUser.getId();

        if (!users.containsKey(id)) {
            String errorMessage = String.format("Пользователь с id %d не найден", id);
            log.error(errorMessage);
            throw new UserNotFoundException(errorMessage);
        }

        User existingUser = users.get(id);
        existingUser.setName(newUser.getName());
        existingUser.setLogin(newUser.getLogin());
        existingUser.setEmail(newUser.getEmail());
        existingUser.setBirthday(newUser.getBirthday());
        checkAndSetName(existingUser);

        log.info("Успешно обработан HTTP-запрос на обновление пользователя: {}", newUser);

        return existingUser;
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private void checkAndSetName(User user) {
        String name = user.getName();
        if (name == null || name.isBlank()) {
            user.setName(user.getLogin());
        }
    }
}