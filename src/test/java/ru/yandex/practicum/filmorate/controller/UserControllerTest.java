package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void shouldCreateUser() {
        User user = new User(null, "test@mail.com", "login123", "Test User",
                LocalDate.of(1990, 1, 1));

        User createdUser = userController.create(user);

        assertNotNull(createdUser.getId());
        assertEquals("test@mail.com", createdUser.getEmail(), "email отличаются");
        assertEquals("login123", createdUser.getLogin(), "логины отличаются");
        assertEquals("Test User", createdUser.getName(), "имена отличаются");
    }

    @Test
    void shouldSetLoginAsNameWhenNameIsBlankOrNull() {
        User firstUser = new User(null, "test@mail.com", "login123", "",
                LocalDate.of(1991, 2, 14));

        User secondUser = new User(null, "test2@mail.com", "login321", null,
                LocalDate.of(1991, 2, 26));

        User createdFirstUser = userController.create(firstUser);
        User createdSecondUser = userController.create(secondUser);

        assertEquals("login123", createdFirstUser.getName(), "Имя и логин отличаются");
        assertEquals("login321", createdSecondUser.getName(), "Имя и логин отличаются");
    }


    @Test
    void shouldUpdateUser() {
        User originalUser = new User(null, "old@mail.com", "oldlogin", "Old Name",
                LocalDate.of(1991, 2, 14));
        User createdUser = userController.create(originalUser);
        Long userId = createdUser.getId();

        User updatedUser = new User(userId, "new@mail.com", "newlogin", "New Name",
                LocalDate.of(1991, 2, 14));

        User result = userController.update(updatedUser);

        assertEquals(userId, result.getId(), "id отличаются");
        assertEquals("new@mail.com", result.getEmail(), "email отличаются");
        assertEquals("newlogin", result.getLogin(), "логины отличаются");
        assertEquals("New Name", result.getName(), "имена отличаются");
    }

    @Test
    void shouldThrowExceptionThenTryUpdateNonExistentUser() {
        User nonExistentUser = new User(1L, "test@mail.com", "login123", "Test",
                LocalDate.of(1991, 1, 1));

        assertThrows(UserNotFoundException.class, () -> userController.update(nonExistentUser),
                "Должно было выбросить UserNotFoundException");
    }

    @Test
    void shouldReturnAllUsers() {
        User firstUser = new User(null, "user1@mail.com", "user1", "User One",
                LocalDate.of(1990, 1, 1));
        User secondUser = new User(null, "user2@mail.com", "user2", "User Two",
                LocalDate.of(1995, 1, 1));

        userController.create(firstUser);
        userController.create(secondUser);
        Collection<User> users = userController.findAll();

        assertEquals(2, users.size(), "Количество пользователей не совпадают");
    }
}