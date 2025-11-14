package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exception.InvalidDurationException;
import ru.yandex.practicum.filmorate.exception.InvalidReleaseDateException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
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
}