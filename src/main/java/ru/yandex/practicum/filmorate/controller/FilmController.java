package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/films")
@RequiredArgsConstructor
public class FilmController {
    private final FilmService filmService;

    @GetMapping
    public Collection<Film> findAll() {
        log.info("Получен HTTP-запрос на получение всех фильмов");
        List<Film> allFilms = filmService.findAll();
        log.info("Успешно обработан HTTP-запрос на получение всех фильмов");
        return allFilms;
    }

    @PostMapping
    public Film create(@RequestBody @Valid Film film) {
        log.info("Получен HTTP-запрос на создание фильма: {}", film);
        Film createdFilm = filmService.create(film);
        log.info("Успешно обработан HTTP-запрос на создание фильма: {}", createdFilm);
        return createdFilm;
    }

    @PutMapping
    public Film update(@RequestBody @Valid Film film) {
        log.info("Получен HTTP-запрос на обновление фильма: {}", film);
        Film updatedFilm = filmService.update(film);
        log.info("Успешно обработан HTTP-запрос на обновление фильма: {}", updatedFilm);
        return updatedFilm;
    }

    @GetMapping("/{id}")
    public Film get(@PathVariable long id) {
        log.info("Получен HTTP-запрос на получение фильма с id: {}", id);
        Film existingFilm = filmService.get(id);
        log.info("Успешно обработан HTTP-запрос на получение фильма с id: {}", id);
        return existingFilm;
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable long id, @PathVariable long userId) {
        log.info("Получен HTTP-запрос на добавление лайка фильму с id = {} от пользователя = {}", id, userId);
        filmService.addLike(id, userId);
        log.info("Успешно обработан HTTP-запрос на добавление лайка фильму с id = {} от пользователя = {}", id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable long id, @PathVariable long userId) {
        log.info("Получен HTTP-запрос на удаление лайка фильму с id = {} от пользователя = {}", id, userId);
        filmService.removeLike(id, userId);
        log.info("Успешно обработан HTTP-запрос на удаление лайка фильму с id = {} от пользователя = {}", id, userId);
    }

    @GetMapping("/popular")
    public List<Film> getPopular(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Integer year) {

        log.info("Получен HTTP-запрос на получение популярных фильмов: count={}, genreId={}, year={}",
                count, genreId, year);

        List<Film> popularFilms = filmService.getPopular(count, genreId, year);

        log.info("Успешно обработан HTTP-запрос, найдено {} фильмов", popularFilms.size());
        return popularFilms;
    }

    @GetMapping("/search")
    public List<Film> searchFilms(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "title") String by) {

        log.info("Получен HTTP-запрос на поиск фильмов: query={}, by={}", query, by);

        // Валидация для 'by'
        if (!isValidSearchParameter(by)) {
            throw new ValidationException("Параметр 'by' должен быть 'title', 'director' или 'director,title'");
        }

        List<Film> foundFilms = filmService.searchFilms(query, by);
        log.info("Успешно обработан HTTP-запрос на поиск, найдено {} фильмов", foundFilms.size());

        return foundFilms;
    }

    private boolean isValidSearchParameter(String by) {
        if (by == null || by.isBlank()) {
            return false;
        }

        // Разделяем по запятой и проверяем каждый параметр
        String[] params = by.split(",");
        for (String param : params) {
            String trimmed = param.trim().toLowerCase();
            if (!trimmed.equals("title") && !trimmed.equals("director")) {
                return false;
            }
        }
        return true;
    }
}