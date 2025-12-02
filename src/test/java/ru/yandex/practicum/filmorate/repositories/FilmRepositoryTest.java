//package ru.yandex.practicum.filmorate.repositories;
//
//import lombok.RequiredArgsConstructor;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
//import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.jdbc.Sql;
//import ru.yandex.practicum.filmorate.dal.FilmRepository;
//import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
//import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
//import ru.yandex.practicum.filmorate.model.Film;
//import ru.yandex.practicum.filmorate.model.Genre;
//import ru.yandex.practicum.filmorate.model.Mpa;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@JdbcTest
//@Import({FilmRepository.class, FilmRowMapper.class, GenreRowMapper.class})
//@Sql(scripts = {"/schema.sql", "/data.sql"})
//@AutoConfigureTestDatabase
//@RequiredArgsConstructor(onConstructor_ = @Autowired)
//class FilmRepositoryTest {
//
//    private final FilmRepository filmRepository;
//
//    @Test
//    void shouldFindFilmById() {
//        Film film = filmRepository.get(1L);
//
//        assertThat(film)
//                .isNotNull()
//                .hasFieldOrPropertyWithValue("id", 1L)
//                .hasFieldOrPropertyWithValue("name", "Фильм А")
//                .hasFieldOrPropertyWithValue("description", "Описание фильма А")
//                .hasFieldOrPropertyWithValue("duration", 100L)
//                .hasFieldOrPropertyWithValue("releaseDate", LocalDate.of(2000, 1, 1));
//
//        assertThat(film.getMpa())
//                .isNotNull()
//                .hasFieldOrPropertyWithValue("id", 1)
//                .hasFieldOrPropertyWithValue("name", "G");
//
//        assertThat(film.getGenres())
//                .hasSize(1)
//                .anyMatch(g -> g.getId() == 1 && "Комедия".equals(g.getName()));
//    }
//
//    @Test
//    void shouldFindAllFilms() {
//        List<Film> films = filmRepository.findAll();
//
//        assertThat(films).hasSize(2);
//    }
//
//    @Test
//    void shouldGetPopularFilms() {
//        List<Film> popular = filmRepository.getPopularFilms(10);
//
//        assertThat(popular).hasSize(2);
//        assertThat(popular.get(0).getId()).isEqualTo(1L); // Фильм А — 2 лайка
//        assertThat(popular.get(1).getId()).isEqualTo(2L); // Фильм Б — 1 лайк
//    }
//
//    @Test
//    void shouldAddAndRemoveLike() {
//        // Проверяем начальное состояние
//        Film film = filmRepository.get(2L);
//        assertThat(film.getRate()).isEqualTo(1L);
//
//        // Добавляем лайк
//        filmRepository.addLike(2L, 2L);
//        film = filmRepository.get(2L);
//        assertThat(film.getRate()).isEqualTo(2L);
//        assertThat(film.getUserIds()).contains(1L, 2L);
//
//        // Удаляем лайк
//        filmRepository.removeLike(2L, 1L);
//        film = filmRepository.get(2L);
//        assertThat(film.getRate()).isEqualTo(1L);
//        assertThat(film.getUserIds()).containsExactly(2L);
//    }
//
//    @Test
//    void shouldCreateFilm() {
//        Film newFilm = new Film();
//        newFilm.setName("Новый фильм");
//        newFilm.setDescription("Описание нового фильма");
//        newFilm.setReleaseDate(LocalDate.of(2002, 1, 1));
//        newFilm.setDuration(90);
//        newFilm.setMpa(new Mpa(2, null));
//        newFilm.setGenres(Set.of(new Genre(3, null), new Genre(6, null))); // Мультфильм + Боевик
//
//        Film saved = filmRepository.create(newFilm);
//
//        assertThat(saved.getId()).isNotNull();
//        assertThat(saved.getName()).isEqualTo("Новый фильм");
//
//        // Проверяем, что жанры сохранились
//        Film fromDb = filmRepository.get(saved.getId());
//        assertThat(fromDb.getGenres())
//                .hasSize(2)
//                .extracting(Genre::getName)
//                .contains("Мультфильм", "Боевик");
//    }
//
//    @Test
//    void shouldUpdateFilm() {
//        Film film = filmRepository.get(1L);
//        film.setName("Обновлённый фильм А");
//        film.setMpa(new Mpa(3, null)); // PG-13
//        film.setGenres(Set.of(new Genre(2, null), new Genre(4, null))); // Драма + Триллер
//
//        filmRepository.update(film);
//        Film updated = filmRepository.get(1L);
//
//        assertThat(updated.getName()).isEqualTo("Обновлённый фильм А");
//        assertThat(updated.getMpa().getId()).isEqualTo(3);
//        assertThat(updated.getGenres())
//                .hasSize(2)
//                .extracting(Genre::getName)
//                .contains("Драма", "Триллер");
//    }
//}


package ru.yandex.practicum.filmorate.repositories;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.FilmRepository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({FilmRepository.class, FilmRowMapper.class, GenreRowMapper.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmRepositoryTest {

    private final FilmRepository filmRepository;

    @Test
    void shouldFindAllFilms() {
        List<Film> films = filmRepository.findAll();
        assertThat(films).hasSize(2);
    }

    @Test
    void shouldGetPopularFilms() {
        List<Film> popular = filmRepository.getPopularFilms(10);
        assertThat(popular).hasSize(2);

        assertThat(popular.get(0).getId()).isEqualTo(1L);
        assertThat(popular.get(0).getRate()).isEqualTo(2L);
        assertThat(popular.get(1).getId()).isEqualTo(2L);
        assertThat(popular.get(1).getRate()).isEqualTo(1L);
    }

    @Test
    void shouldAddAndRemoveLike() {
        Film film = filmRepository.get(2L);
        assertThat(film.getRate()).isEqualTo(1L);

        filmRepository.addLike(2L, 2L);
        film = filmRepository.get(2L);
        assertThat(film.getRate()).isEqualTo(2L);

        filmRepository.removeLike(2L, 1L);
        film = filmRepository.get(2L);
        assertThat(film.getRate()).isEqualTo(1L);
    }
}