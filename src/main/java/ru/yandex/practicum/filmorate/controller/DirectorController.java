package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@Validated
public class DirectorController {

    private final DirectorService directorService;

    @GetMapping("/directors")
    public Collection<Director> getAllDirectors() {
        return directorService.getAllDirectors();
    }

    @GetMapping("/directors/{id}")
    public Director getDirectorById(@PathVariable("id") @NotNull(message = "id режиссера должно быть указано") Long directorId) {
        return directorService.getDirectorById(directorId);
    }

    @PostMapping("/directors")
    public Director createDirector(@NotNull(message = "Передано пустое значение Director") @RequestBody Director director) {
        return directorService.createDirector(director);
    }

    @PutMapping("/directors")
    public Director updateDirector(@NotNull(message = "Передано пустое значение Director") @RequestBody Director director) {
        return directorService.updateDirector(director);
    }

    @DeleteMapping("/directors/{id}")
    public void deleteDirector(@PathVariable("id") @NotNull(message = "id режиссера должно быть указано") Long directorId) {
        directorService.deleteDirector(directorId);
    }
}