package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.FilmRepository;
import ru.yandex.practicum.filmorate.dal.GenreRepository;
import ru.yandex.practicum.filmorate.dal.MpaRepository;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exception.InvalidDurationException;
import ru.yandex.practicum.filmorate.exception.InvalidReleaseDateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate FILM_BIRTHDAY = LocalDate.of(1895, 12, 28);

    private final FilmRepository filmRepository;
    private final GenreRepository genreRepository;
    private final MpaRepository mpaRepository;

    public Film create(Film film) {
        validate(film);
        validateGenres(film.getGenreIds());
        Film saved = filmRepository.create(film);
        return filmRepository.get(saved.getId());
    }

    public Film get(long id) {
        if (!filmRepository.exists(id)) {
            throw new FilmNotFoundException("Film with id=" + id + " not found");
        }
        return filmRepository.get(id);
    }

    public List<Film> findAll() {
        return filmRepository.findAll();
    }

    public Film update(Film film) {
        validate(film);
        if (!filmRepository.exists(film.getId())) {
            throw new FilmNotFoundException("Film with id=" + film.getId() + " not found");
        }
        validateGenres(film.getGenreIds());
        Film updated = filmRepository.update(film);
        filmRepository.updateGenres(updated.getId(), film.getGenreIds());
        return filmRepository.get(updated.getId());
    }

    public void addLike(long filmId, long userId) {
        if (!filmRepository.exists(filmId)) {
            throw new FilmNotFoundException("Film with id=" + filmId + " not found");
        }
        filmRepository.addLike(filmId, userId);
    }

    public void removeLike(long filmId, long userId) {
        if (!filmRepository.exists(filmId)) {
            throw new FilmNotFoundException("Film with id=" + filmId + " not found");
        }
        filmRepository.removeLike(filmId, userId);
    }

    public List<Film> getPopular(int count) {
        return filmRepository.getPopularFilms(count);
    }

    private void validate(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Описание не должно превышать 200 символов");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(FILM_BIRTHDAY)) {
            throw new InvalidReleaseDateException("Дата релиза не может быть раньше 28.12.1895");
        }
        if (film.getDuration() <= 0) {
            throw new InvalidDurationException("Продолжительность должна быть положительной");
        }
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("Рейтинг MPA обязателен");
        }
        if (mpaRepository.findById(film.getMpa().getId()).isEmpty()) {
            throw new NotFoundException("Неизвестный рейтинг MPA: " + film.getMpa().getId());
        }
    }

    private void validateGenres(Set<Integer> genreIds) {
        if (genreIds == null) return;
        List<Integer> invalid = genreIds.stream()
                .filter(id -> genreRepository.findById(id).isEmpty())
                .toList();
        if (!invalid.isEmpty()) {
            throw new NotFoundException("Жанры не найдены: " + invalid);
        }
    }
}