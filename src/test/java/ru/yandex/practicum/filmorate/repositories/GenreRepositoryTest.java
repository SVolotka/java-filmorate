package ru.yandex.practicum.filmorate.repositories;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dal.GenreRepository;
import ru.yandex.practicum.filmorate.dal.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({GenreRepository.class, GenreRowMapper.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GenreRepositoryTest {

    private final GenreRepository genreRepository;

    @Test
    void shouldFindAllGenres() {
        List<Genre> genres = genreRepository.findAll();

        assertThat(genres).hasSize(6);
        assertThat(genres)
                .extracting(Genre::getId)
                .contains(1, 2, 3, 4, 5, 6);
    }

    @Test
    void shouldFindGenreById() {
        Optional<Genre> genre = genreRepository.findById(1);

        assertThat(genre)
                .isPresent()
                .hasValueSatisfying(g -> {
                    assertThat(g.getId()).isEqualTo(1);
                    assertThat(g.getName()).isEqualTo("Комедия");
                });
    }

    @Test
    void shouldReturnEmptyForNonExistentGenre() {
        Optional<Genre> genre = genreRepository.findById(999);

        assertThat(genre).isEmpty();
    }
}