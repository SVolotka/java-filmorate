package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.InvalidDurationException;
import ru.yandex.practicum.filmorate.exception.InvalidReleaseDateException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilmService {
    private static final LocalDate FILM_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private static final int Max_Name_Size = 200;
    public static final Comparator<Film> FILM_COMPARATOR = Comparator.comparingLong(Film::getRate).reversed();
    private long counter = 0L;

    FilmStorage filmStorage;
    UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film create(Film film) {
        validate(film);
        film.setId(++counter);
        if (film.getUserIds() == null) {
            film.setUserIds(new HashSet<>());
        }
        filmStorage.create(film);
        return film;
    }

    public Film get(long id) {
        return filmStorage.get(id);
    }

    public List<Film> findAll() {
        return new ArrayList<>(filmStorage.getAll());
    }

    public Film update(Film film) {
        Film existingFilm = filmStorage.get(film.getId());
        validate(film);

        if (film.getUserIds() == null) {
            film.setUserIds(existingFilm.getUserIds());
        }
        if (film.getRate() == null) {
            film.setRate(existingFilm.getRate());
        }

        filmStorage.update(film);
        return film;
    }

    public void addLike(long filmId, long userId) {
        final Film film = filmStorage.get(filmId);
        userStorage.get(userId);
        if (film.getUserIds() == null) {
            film.setUserIds(new HashSet<>());
        }
        film.getUserIds().add(userId);
        film.setRate((long) film.getUserIds().size());
    }

    public void removeLike(long filmId, long userId) {
        final Film film = filmStorage.get(filmId);
        userStorage.get(userId);
        if (film.getUserIds() != null) {
            film.getUserIds().remove(userId);
            film.setRate((long) film.getUserIds().size());
        }
    }

    public List<Film> getPopular(int count) {
        return filmStorage.getAll().stream()
                .sorted(FILM_COMPARATOR)
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validate(Film film) {
        if (film.getName() == null || film.getName().isEmpty()) {
            throw new ValidationException("File name invalid");
        }
        if (film.getDescription() != null && film.getDescription().length() > Max_Name_Size) {
            throw new ValidationException("Film description invalid");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(FILM_BIRTHDAY)) {
            throw new InvalidReleaseDateException("Film release date invalid");
        }
        if (film.getDuration() <= 0) {
            throw new InvalidDurationException("Film duration invalid");
        }
    }
}