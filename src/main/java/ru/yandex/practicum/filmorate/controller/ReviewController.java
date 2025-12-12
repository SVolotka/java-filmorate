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
    public Collection<Review> getAllReviews() {
        return reviewService.getAllReviews();
    }

    @PostMapping
    public Review add(@RequestBody Review review) {
        return reviewService.add(review);
    }

    @DeleteMapping("/id")
    public void delete(@PathVariable("id") Integer id) {
        reviewService.delete(id);
    }
}
