package ru.yandex.practicum.filmorate.repositories;//package ru.yandex.practicum.filmorate.repositories;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.DirectorRepository;
import ru.yandex.practicum.filmorate.dal.FilmRepository;
import ru.yandex.practicum.filmorate.dal.GenreRepository;
import ru.yandex.practicum.filmorate.dal.MpaRepository;
import ru.yandex.practicum.filmorate.dal.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.MpaRowMapper;
import ru.yandex.practicum.filmorate.dal.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({FilmRepository.class, FilmRowMapper.class, GenreRowMapper.class, MpaRepository.class, GenreRepository.class, MpaRowMapper.class, UserRowMapper.class, DirectorRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql({"/schema.sql", "/data.sql"})
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmRepositoryTest {

    private final FilmRepository filmRepository;
    private final GenreRepository genreRepository;

    @Test
    void shouldFindAllFilms() {
        List<Film> films = filmRepository.findAll();
        assertThat(films).hasSize(2);
    }

    /*@Test
    void shouldGetPopularFilms() {
        List<Film> popular = filmRepository.getPopularFilms(10);
        assertThat(popular).hasSize(2);

        assertThat(popular.get(0).getId()).isEqualTo(1L);
        assertThat(popular.get(0).getRate()).isEqualTo(2L);
        assertThat(popular.get(1).getId()).isEqualTo(2L);
        assertThat(popular.get(1).getRate()).isEqualTo(1L);
    }*/

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

    @Test
    void shouldDeletedFilmByIdWhenExistsFilm() {
        Film film = filmRepository.get(2L);
        List<Genre> genresBefore = genreRepository.getGenresByFilmId(2L);

        assertThat(film).isNotNull();
        assertThat(film.getName()).isEqualTo("Фильм Б");
        assertThat(genresBefore.size()).isEqualTo(2);

        filmRepository.deleteFilmById(2L);
        List<Genre> genresAfter = genreRepository.getGenresByFilmId(2L);
        assertThat(filmRepository.exists(2L)).isFalse();
        assertThat(genresAfter.size()).isEqualTo(0);
    }

    @Test
    void shouldNotAffectOtherFilmsWhenDeletingOneFilm() {
        List<Film> allFilmsBefore = filmRepository.findAll();
        assertThat(allFilmsBefore).hasSize(2);

        long filmIdToDelete = 1L;
        long otherFilmId = 2L;

        Film filmToDelete = filmRepository.get(filmIdToDelete);
        Film otherFilm = filmRepository.get(otherFilmId);

        assertThat(filmToDelete.getName()).isEqualTo("Фильм А");
        assertThat(otherFilm.getName()).isEqualTo("Фильм Б");

        filmRepository.deleteFilmById(filmIdToDelete);

        List<Film> allFilmsAfter = filmRepository.findAll();
        assertThat(allFilmsAfter).hasSize(1);

        Film remainingFilm = filmRepository.get(otherFilmId);
        assertThat(remainingFilm).isNotNull();
        assertThat(remainingFilm.getName()).isEqualTo("Фильм Б");

        assertThat(remainingFilm.getRate()).isEqualTo(1L);

        assertThat(remainingFilm.getGenres()).hasSize(2);
    }
}