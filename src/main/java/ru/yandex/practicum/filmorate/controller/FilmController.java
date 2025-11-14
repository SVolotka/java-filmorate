package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exception.InvalidDurationException;
import ru.yandex.practicum.filmorate.exception.InvalidReleaseDateException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/films")
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @GetMapping
    public Collection<Film> findAll() {
        log.info("Получен HTTP-запрос на получение всех фильмов");
        return new ArrayList<>(films.values());
    }

    @PostMapping
    public Film create(@RequestBody @Valid Film film) {
        log.info("Получен HTTP-запрос на создание фильма: {}", film);

        validateReleaseDate(film.getReleaseDate());
        validateDuration(film.getDuration());
        film.setId(getNextId());
        films.put(film.getId(), film);

        log.info("Успешно обработан HTTP-запрос на создание фильма: {}", film);
        return film;
    }

    @PutMapping
    public Film update(@RequestBody @Valid Film newFilm) {
        log.info("Получен HTTP-запрос на обновление фильма: {}", newFilm);

        Long id = newFilm.getId();

        if (!films.containsKey(id)) {
            String errorMessage = String.format("Фильм с id %d не найден", id);
            log.error(errorMessage);
            throw new FilmNotFoundException(errorMessage);
        }

        Film existingFilm = films.get(newFilm.getId());

        validateReleaseDate(newFilm.getReleaseDate());
        validateDuration(newFilm.getDuration());
        existingFilm.setName(newFilm.getName());
        existingFilm.setDescription(newFilm.getDescription());
        existingFilm.setDuration(newFilm.getDuration());
        existingFilm.setReleaseDate(newFilm.getReleaseDate());

        log.info("Успешно обработан HTTP-запрос на обновление фильма: {}", newFilm);
        return existingFilm;
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private void validateReleaseDate(LocalDate releaseDate) {
        if (releaseDate == null) {
            throw new InvalidReleaseDateException("Дата релиза не может быть null");
        }
        if (releaseDate.isBefore(MIN_RELEASE_DATE)) {
            throw new InvalidReleaseDateException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }

    private void validateDuration(long duration) {
        if (duration <= 0) {
            throw new InvalidDurationException("Продолжительность фильма должна быть 0");
        }
    }
}