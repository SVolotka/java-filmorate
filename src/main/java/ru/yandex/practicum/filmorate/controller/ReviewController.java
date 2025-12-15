package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.Collection;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public Collection<Review> getAllReviews(
            @RequestParam(required = false) Integer filmId,
            @RequestParam(defaultValue =  "10", required = false) Integer count
    ) {
        return reviewService.getAllReviews(filmId, count);
    }

    @PostMapping
    public Review add(@RequestBody Review review) {
        return reviewService.add(review);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Integer id) {
        reviewService.delete(id);
    }

    @GetMapping("/{id}")
    public Review getById(@PathVariable("id") Integer id) {
        return reviewService.getById(id);
    }

    @PutMapping
    public Review update(@RequestBody Review review) {
        return reviewService.update(review);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addUserLike(@PathVariable("id") Integer id, @PathVariable("userId") Integer userId) {
        reviewService.addLike(id, userId, true);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public void addUserDislike(@PathVariable("id") Integer id, @PathVariable("userId") Integer userId) {
        reviewService.addDislike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deleteUserLike(@PathVariable("id") Integer id, @PathVariable("userId") Integer userId) {
        reviewService.deleteLike(id, userId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public void deleteUserDislike(@PathVariable("id") Integer id, @PathVariable("userId") Integer userId) {
        reviewService.deleteDislike(id, userId);
    }
}
