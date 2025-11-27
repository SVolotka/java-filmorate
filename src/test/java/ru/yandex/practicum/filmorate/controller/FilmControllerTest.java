package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exception.InvalidDurationException;
import ru.yandex.practicum.filmorate.exception.InvalidReleaseDateException;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;
    private UserController userController;

    @BeforeEach
    void setUp() {
        FilmStorage filmStorage = new InMemoryFilmStorage();
        UserStorage userStorage = new InMemoryUserStorage();
        FilmService filmService = new FilmService(filmStorage, userStorage);
        UserService userService = new UserService(userStorage, filmStorage);
        filmController = new FilmController(filmService);
        userController = new UserController(userService);
    }

    @Test
    void shouldCreateFilm() {
        Film film = new Film(null, "Test Film", "Test Description",
                LocalDate.of(2000, 1, 1), 120);


        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm.getId(), "id не должно быть null");
        assertEquals("Test Film", createdFilm.getName(), "Название фильма отличаются");
        assertEquals("Test Description", createdFilm.getDescription(), "Описание фильма отличаются");
        assertEquals(120, createdFilm.getDuration(), "Продолжительность фильма отличаются");
    }

    @Test
    void shouldCreateFilmWithMinReleaseDate() {
        Film film = new Film(null, "Test Film", "Test Description",
                LocalDate.of(1895, 12, 28), 120);

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm, "Фильм не создан");
        assertEquals(LocalDate.of(1895, 12, 28), createdFilm.getReleaseDate(),
                "Дата релиза фильма отличаются");
    }

    @Test
    void shouldThrowExceptionWithInvalidReleaseDate() {
        Film firstFilm = new Film(null, "Test Film", "Test Description",
                LocalDate.of(1890, 1, 1), 120);

        Film secondFilm = new Film(null, "Test Film2", "Test Description2",
                null, 120);

        assertThrows(InvalidReleaseDateException.class, () -> filmController.create(firstFilm),
                "Должно было выбросить InvalidReleaseDateException");
        assertThrows(InvalidReleaseDateException.class, () -> filmController.create(secondFilm),
                "Должно было выбросить InvalidReleaseDateException");
    }

    @Test
    void shouldThrowExceptionThenTryCreateFilmWithNegativeDuration() {
        Film film = new Film(null, "Test Film", "Test Description",
                LocalDate.of(2000, 1, 1), -120);

        assertThrows(InvalidDurationException.class, () -> filmController.create(film),
                "Должно было выбросить InvalidDurationException");
    }

    @Test
    void shouldUpdateFilm() {
        Film originalFilm = new Film(null, "Old Film", "Old Description",
                LocalDate.of(2000, 1, 1), 120);
        Film createdFilm = filmController.create(originalFilm);
        Long filmId = createdFilm.getId();

        Film updatedFilm = new Film(filmId, "New Film", "New Description",
                LocalDate.of(2002, 2, 2), 100);

        Film result = filmController.update(updatedFilm);


        assertEquals(filmId, result.getId(), "id отличаются");
        assertEquals("New Film", result.getName(), "название отличаются");
        assertEquals("New Description", result.getDescription(), "описание отличаются");
        assertEquals(100, result.getDuration(), "продолжительность отличаются");
    }

    @Test
    void shouldThrowExceptionThenTryUpdateNonExistentFilm() {
        Film nonExistentFilm = new Film(1L, "Test Film", "Test Description",
                LocalDate.of(2000, 1, 1), 120);

        assertThrows(FilmNotFoundException.class, () -> filmController.update(nonExistentFilm),
                "Должно было выбросить FilmNotFoundException");
    }

    @Test
    void shouldReturnAllFilms() {
        Film firstFilm = new Film(null, "Film One", "Description One",
                LocalDate.of(2000, 1, 1), 120);
        Film secondFilm = new Film(null, "Film Two", "Description Two",
                LocalDate.of(2002, 2, 2), 100);

        filmController.create(firstFilm);
        filmController.create(secondFilm);

        Collection<Film> films = filmController.findAll();

        assertEquals(2, films.size(), "Количество фильмов не совпадают");
    }

    @Test
    void shouldGetFilmById() {
        Film film = new Film(null, "Test Film", "Test Description",
                LocalDate.of(2000, 1, 1), 120);
        Film createdFilm = filmController.create(film);
        Long filmId = createdFilm.getId();

        Film foundFilm = filmController.get(filmId);

        assertNotNull(foundFilm);
        assertEquals(filmId, foundFilm.getId());
        assertEquals("Test Film", foundFilm.getName());
    }

    @Test
    void shouldThrowExceptionWhenGetNonExistentFilm() {
        assertThrows(FilmNotFoundException.class, () -> filmController.get(999L),
                "Должно было выбросить FilmNotFoundException при получении несуществующего фильма");
    }

    @Test
    void shouldAddAndRemoveLike() {
        Film film = new Film(null, "Test Film", "Test Description",
                LocalDate.of(2000, 1, 1), 120);
        Film createdFilm = filmController.create(film);
        Long filmId = createdFilm.getId();

        User user = new User(null, "user@mail.com", "userlogin", "User Name",
                LocalDate.of(1990, 1, 1));
        User createdUser = userController.create(user);
        Long userId = createdUser.getId();

        filmController.addLike(filmId, userId);

        Film filmAfterLike = filmController.get(filmId);
        assertEquals(1, filmAfterLike.getRate(), "Рейтинг должен быть 1 после добавления лайка");

        filmController.removeLike(filmId, userId);

        Film filmAfterRemoveLike = filmController.get(filmId);
        assertEquals(0, filmAfterRemoveLike.getRate(), "Рейтинг должен быть 0 после удаления лайка");
    }

    @Test
    void shouldThrowExceptionWhenAddLikeToNonExistentFilm() {
        User user = new User(null, "user@mail.com", "userlogin", "User Name",
                LocalDate.of(1990, 1, 1));
        User createdUser = userController.create(user);
        Long userId = createdUser.getId();

        assertThrows(FilmNotFoundException.class,
                () -> filmController.addLike(999L, userId),
                "Должно было выбросить FilmNotFoundException при добавлении лайка несуществующему фильму");
    }

    @Test
    void shouldThrowExceptionWhenAddLikeFromNonExistentUser() {
        Film film = new Film(null, "Test Film", "Test Description",
                LocalDate.of(2000, 1, 1), 120);
        Film createdFilm = filmController.create(film);
        Long filmId = createdFilm.getId();

        assertThrows(UserNotFoundException.class,
                () -> filmController.addLike(filmId, 999L),
                "Должно было выбросить UserNotFoundException при добавлении лайка от несуществующего пользователя");
    }

    @Test
    void shouldReturnPopularFilms() {
        Film firstFilm = new Film(null, "Film One", "Description One",
                LocalDate.of(2000, 1, 1), 120);
        Film secondFilm = new Film(null, "Film Two", "Description Two",
                LocalDate.of(2002, 2, 2), 100);
        Film thirdFilm = new Film(null, "Film Three", "Description Three",
                LocalDate.of(2003, 3, 3), 90);

        Film createdFirst = filmController.create(firstFilm);
        Film createdSecond = filmController.create(secondFilm);
        Film createdThird = filmController.create(thirdFilm);

        User user1 = new User(null, "user1@mail.com", "user1", "User One",
                LocalDate.of(1990, 1, 1));
        User user2 = new User(null, "user2@mail.com", "user2", "User Two",
                LocalDate.of(1995, 1, 1));
        User createdUser1 = userController.create(user1);
        User createdUser2 = userController.create(user2);

        filmController.addLike(createdSecond.getId(), createdUser1.getId());
        filmController.addLike(createdSecond.getId(), createdUser2.getId());
        filmController.addLike(createdFirst.getId(), createdUser1.getId());

        List<Film> popularFilms = filmController.getPopular(2);

        assertEquals(2, popularFilms.size(), "Должно вернуть 2 популярных фильма");
        assertEquals("Film Two", popularFilms.get(0).getName(), "Первый фильм должен быть самым популярным");
        assertEquals("Film One", popularFilms.get(1).getName(), "Второй фильм должен быть вторым по популярности");
    }

    @Test
    void shouldReturnPopularFilmsWithDefaultCount() {
        for (int i = 1; i <= 15; i++) {
            Film film = new Film(null, "Film " + i, "Description " + i,
                    LocalDate.of(2000 + i, 1, 1), 100 + i);
            filmController.create(film);
        }

        List<Film> popularFilms = filmController.getPopular(10);

        assertEquals(10, popularFilms.size(), "Должно вернуть 10 фильмов по умолчанию");
    }

    @Test
    void shouldHandleEmptyFilmList() {
        Collection<Film> films = filmController.findAll();

        assertTrue(films.isEmpty(), "Список фильмов должен быть пустым при инициализации");
    }

    @Test
    void shouldThrowExceptionWhenCreateFilmWithEmptyName() {
        Film film = new Film(null, "", "Test Description",
                LocalDate.of(2000, 1, 1), 120);

        assertThrows(ValidationException.class, () -> filmController.create(film),
                "Должно было выбросить ValidationException при создании фильма с пустым названием");
    }

    @Test
    void shouldThrowExceptionWhenCreateFilmWithNullName() {
        Film film = new Film(null, null, "Test Description",
                LocalDate.of(2000, 1, 1), 120);

        assertThrows(ValidationException.class, () -> filmController.create(film),
                "Должно было выбросить ValidationException при создании фильма с null названием");
    }

    @Test
    void shouldThrowExceptionWhenCreateFilmWithLongDescription() {
        String longDescription = "A".repeat(201); // 201 символ
        Film film = new Film(null, "Test Film", longDescription,
                LocalDate.of(2000, 1, 1), 120);

        assertThrows(ValidationException.class, () -> filmController.create(film),
                "Должно было выбросить ValidationException при создании фильма с описанием длиннее 200 символов");
    }

    @Test
    void shouldCreateFilmWithMaxDescriptionLength() {
        String maxDescription = "A".repeat(200);
        Film film = new Film(null, "Test Film", maxDescription,
                LocalDate.of(2000, 1, 1), 120);

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm);
        assertEquals(maxDescription, createdFilm.getDescription(),
                "Описание должно сохраниться полностью");
    }

    @Test
    void shouldRemoveLikeFromFilmWithoutLikes() {
        Film film = new Film(null, "Test Film", "Test Description",
                LocalDate.of(2000, 1, 1), 120);
        Film createdFilm = filmController.create(film);
        Long filmId = createdFilm.getId();

        User user = new User(null, "user@mail.com", "userlogin", "User Name",
                LocalDate.of(1990, 1, 1));
        User createdUser = userController.create(user);
        Long userId = createdUser.getId();

        assertDoesNotThrow(() -> filmController.removeLike(filmId, userId),
                "Удаление несуществующего лайка не должно вызывать исключение");

        Film filmAfterRemove = filmController.get(filmId);
        assertEquals(0, filmAfterRemove.getRate(), "Рейтинг должен остаться 0");
    }
}