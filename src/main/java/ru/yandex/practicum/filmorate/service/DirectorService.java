package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.DirectorRepository;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectorService {

    private final DirectorRepository directorRepository;

    public Collection<Director> getAllDirectors() {
        log.info("Отправляем запрос на получние списка всех режиссеров ...");
        Collection<Director> listOfAllDirectors = directorRepository.getAllDirectors().values();
        log.info("Список всех режиссеров отправлен клиенту.");
        return listOfAllDirectors;
    }

    public Director getDirectorById(Long directorId) {
        log.info("Отправляем запрос на получение информации о режиссере с id = {} ...", directorId);
        Director director = directorRepository.getDirectorById(directorId);
        log.info("Информация о режиссере с id = {} отправлена клиенту.", directorId);
        return director;
    }

    public Director createDirector(Director director) {
        log.info("Отправляем запрос на создание в БД записи о режиссере ...");
        Director newDirector = directorRepository.createDirector(director);
        log.info("Режиссер добавлен в БД.");
        return newDirector;
    }

    public Director updateDirector(Director director) {
        log.info("Отправляем запрос на обновление данных о режиссере ...");
        Director updatedDirector = directorRepository.updateDirector(director);
        log.info("Информация о фильме с id = {} обновлена", director.getId());
        return updatedDirector;
    }

    public void deleteDirector(Long directorId) {
        log.info("Отправляем запрос на удаление режиссера с id {}", directorId);
        directorRepository.deleteDirector(directorId);
        log.info("Запись о режиссере с id = {} удалена из БД.", directorId);
    }
}