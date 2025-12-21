package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class Review {
    private Long reviewId;
    @NotNull
    private String content;
    private Boolean isPositive;
    private Long userId;
    private Long filmId;
    private Integer useful = 0;
}