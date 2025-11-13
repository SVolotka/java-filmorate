package ru.yandex.practicum.filmorate.exception;

public class InvalidDurationException extends RuntimeException {
    public InvalidDurationException(String message) {
        super(message);
    }
}
