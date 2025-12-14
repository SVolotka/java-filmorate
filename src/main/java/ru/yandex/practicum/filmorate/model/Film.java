package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Film {
    private Long id;
    @NotBlank(message = "Название фильма не может быть null или пустым")
    private String name;
    @Size(max = 200, message = "Длина описания не должна превышать 200 символов")
    private String description;
    @NotNull(message = "Дата релиза не может быть null")
    private LocalDate releaseDate;
    @Positive(message = "Продолжительность фильма должна быть положительной")
    private long duration;

    @NotNull(message = "Рейтинг MPA обязателен")
    private Mpa mpa;

    private Set<Genre> genres = new LinkedHashSet<>();

    @JsonIgnore
   private Set<Long> userIds = new HashSet<>();

    @JsonIgnore
    private Long rate = 0L;

    public void setGenres(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            this.genres = new LinkedHashSet<>();
        } else {
            this.genres = new LinkedHashSet<>(genres);
        }
    }

    public Integer getMpaId() {
        return mpa != null ? mpa.getId() : null;
    }

    public Set<Integer> getGenreIds() {
        return genres.stream()
                .map(Genre::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}