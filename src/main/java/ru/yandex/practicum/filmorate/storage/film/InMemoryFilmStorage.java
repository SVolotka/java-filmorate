package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.FilmAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {
    private final HashMap<Long, Film> films = new HashMap<>();

    @Override
    public void create(Film film) {
        if (films.containsKey(film.getId())) {
            String errorMessage = String.format("Film with id %d already exist", film.getId());
            throw new FilmAlreadyExistException(errorMessage);
        }
        films.put(film.getId(), film);
    }

    @Override
    public void update(Film film) {
        if (!films.containsKey(film.getId())) {
            String errorMessage = String.format("Film with id %d not found", film.getId());
            throw new FilmNotFoundException(errorMessage);
        }
        films.put(film.getId(), film);
    }

    @Override
    public Film get(long id) {
        if (!films.containsKey(id)) {
            String errorMessage = String.format("Film with id %d not found", id);
            throw new FilmNotFoundException(errorMessage);
        }
        return films.get(id);
    }

    @Override
    public List<Film> getAll() {
        return new ArrayList<>(films.values());
    }
}