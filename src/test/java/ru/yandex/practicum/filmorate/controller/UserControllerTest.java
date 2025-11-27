package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        UserStorage userStorage = new InMemoryUserStorage();
        FilmStorage filmStorage = new InMemoryFilmStorage();
        UserService userService = new UserService(userStorage, filmStorage);
        userController = new UserController(userService);
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

        @Test
        void shouldGetUserById() {
            User user = new User(null, "test@mail.com", "login123", "Test User",
                    LocalDate.of(1990, 1, 1));
            User createdUser = userController.create(user);
            Long userId = createdUser.getId();

            User foundUser = userController.get(userId);

            assertNotNull(foundUser);
            assertEquals(userId, foundUser.getId());
            assertEquals("test@mail.com", foundUser.getEmail());
        }

        @Test
        void shouldThrowExceptionWhenGetNonExistentUser() {
            assertThrows(UserNotFoundException.class, () -> userController.get(999L),
                    "Должно было выбросить UserNotFoundException при получении несуществующего пользователя");
        }

        @Test
        void shouldAddAndRemoveFriend() {
            User firstUser = new User(null, "user1@mail.com", "user1", "User One",
                    LocalDate.of(1990, 1, 1));
            User secondUser = new User(null, "user2@mail.com", "user2", "User Two",
                    LocalDate.of(1995, 1, 1));

            User createdFirst = userController.create(firstUser);
            User createdSecond = userController.create(secondUser);

            userController.addFriend(createdFirst.getId(), createdSecond.getId());

            List<User> firstUserFriends = userController.getFriendById(createdFirst.getId());
            assertEquals(1, firstUserFriends.size(), "У первого пользователя должен быть 1 друг");
            assertEquals(createdSecond.getId(), firstUserFriends.getFirst().getId(), "Друг должен быть вторым пользователем");

            List<User> secondUserFriends = userController.getFriendById(createdSecond.getId());
            assertEquals(1, secondUserFriends.size(), "У второго пользователя должен быть 1 друг");
            assertEquals(createdFirst.getId(), secondUserFriends.getFirst().getId(), "Друг должен быть первым пользователем");

            userController.deleteFriend(createdFirst.getId(), createdSecond.getId());

            List<User> firstUserFriendsAfterRemove = userController.getFriendById(createdFirst.getId());
            assertTrue(firstUserFriendsAfterRemove.isEmpty(), "У первого пользователя не должно быть друзей после удаления");

            List<User> secondUserFriendsAfterRemove = userController.getFriendById(createdSecond.getId());
            assertTrue(secondUserFriendsAfterRemove.isEmpty(), "У второго пользователя не должно быть друзей после удаления");
        }

        @Test
        void shouldThrowExceptionWhenAddFriendToNonExistentUser() {
            User existingUser = new User(null, "user1@mail.com", "user1", "User One",
                    LocalDate.of(1990, 1, 1));
            User createdUser = userController.create(existingUser);

            assertThrows(UserNotFoundException.class,
                    () -> userController.addFriend(createdUser.getId(), 999L),
                    "Должно было выбросить UserNotFoundException при добавлении несуществующего друга");
        }

        @Test
        void shouldReturnCommonFriends() {
            User firstUser = new User(null, "user1@mail.com", "user1", "User One",
                    LocalDate.of(1990, 1, 1));
            User secondUser = new User(null, "user2@mail.com", "user2", "User Two",
                    LocalDate.of(1995, 1, 1));
            User commonFriend = new User(null, "common@mail.com", "common", "Common Friend",
                    LocalDate.of(1992, 1, 1));

            User createdFirst = userController.create(firstUser);
            User createdSecond = userController.create(secondUser);
            User createdCommon = userController.create(commonFriend);

            userController.addFriend(createdFirst.getId(), createdCommon.getId());
            userController.addFriend(createdSecond.getId(), createdCommon.getId());

            List<User> commonFriends = userController.getCommonFriends(createdFirst.getId(), createdSecond.getId());

            assertEquals(1, commonFriends.size(), "Должен быть 1 общий друг");
            assertEquals(createdCommon.getId(), commonFriends.getFirst().getId(), "Общий друг должен совпадать");
        }

        @Test
        void shouldReturnEmptyListWhenNoCommonFriends() {
            User firstUser = new User(null, "user1@mail.com", "user1", "User One",
                    LocalDate.of(1990, 1, 1));
            User secondUser = new User(null, "user2@mail.com", "user2", "User Two",
                    LocalDate.of(1995, 1, 1));
            User firstFriend = new User(null, "friend1@mail.com", "friend1", "Friend One",
                    LocalDate.of(1992, 1, 1));
            User secondFriend = new User(null, "friend2@mail.com", "friend2", "Friend Two",
                    LocalDate.of(1993, 1, 1));

            User createdFirst = userController.create(firstUser);
            User createdSecond = userController.create(secondUser);
            User createdFirstFriend = userController.create(firstFriend);
            User createdSecondFriend = userController.create(secondFriend);

            userController.addFriend(createdFirst.getId(), createdFirstFriend.getId());
            userController.addFriend(createdSecond.getId(), createdSecondFriend.getId());

            List<User> commonFriends = userController.getCommonFriends(createdFirst.getId(), createdSecond.getId());
            assertTrue(commonFriends.isEmpty(), "Не должно быть общих друзей");
        }

        @Test
        void shouldThrowExceptionWhenGetCommonFriendsForNonExistentUsers() {
            User existingUser = new User(null, "user1@mail.com", "user1", "User One",
                    LocalDate.of(1990, 1, 1));
            User createdUser = userController.create(existingUser);

            assertThrows(UserNotFoundException.class,
                    () -> userController.getCommonFriends(createdUser.getId(), 999L),
                    "Должно было выбросить UserNotFoundException при поиске общих друзей с несуществующим пользователем");
        }

        @Test
        void shouldThrowExceptionWhenAddSelfAsFriend() {
            User user = new User(null, "user1@mail.com", "user1", "User One",
                    LocalDate.of(1990, 1, 1));
            User createdUser = userController.create(user);

            assertThrows(ValidationException.class,
                    () -> userController.addFriend(createdUser.getId(), createdUser.getId()),
                    "Должно было выбросить ValidationException при добавлении себя в друзья");
        }

        @Test
        void shouldHandleEmptyFriendsList() {
            User user = new User(null, "user1@mail.com", "user1", "User One",
                    LocalDate.of(1990, 1, 1));
            User createdUser = userController.create(user);

            List<User> friends = userController.getFriendById(createdUser.getId());
            assertTrue(friends.isEmpty(), "Список друзей нового пользователя должен быть пустым");
        }

        @Test
        void shouldNotCreateUserWithFutureBirthday() {
            User user = new User(null, "test@mail.com", "login123", "Test User",
                    LocalDate.now().plusDays(1)); // Дата рождения в будущем

            assertThrows(ValidationException.class, () -> userController.create(user),
                    "Должно было выбросить ValidationException при создании пользователя с датой рождения в будущем");
        }

        @Test
        void shouldNotCreateUserWithLoginContainingSpaces() {
            User user = new User(null, "test@mail.com", "login with spaces", "Test User",
                    LocalDate.of(1990, 1, 1));

            assertThrows(ValidationException.class, () -> userController.create(user),
                    "Должно было выбросить ValidationException при создании пользователя с логином содержащим пробелы");
        }

        @Test
        void shouldNotCreateUserWithInvalidEmail() {
            User user = new User(null, "invalid-email", "login123", "Test User",
                    LocalDate.of(1990, 1, 1));

            assertThrows(ValidationException.class, () -> userController.create(user),
                    "Должно было выбросить ValidationException при создании пользователя с невалидным email");
        }
    }