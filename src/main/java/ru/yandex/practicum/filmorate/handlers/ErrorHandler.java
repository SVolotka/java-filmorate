package ru.yandex.practicum.filmorate.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.FilmAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exception.InvalidDurationException;
import ru.yandex.practicum.filmorate.exception.InvalidReleaseDateException;
import ru.yandex.practicum.filmorate.exception.UserAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.ErrorResponse;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFoundException(UserNotFoundException exception) {
        log.error(exception.getMessage());
        return ErrorResponse.builder().errorCode(HttpStatus.NOT_FOUND.value()).description(exception.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleFilmNotFoundException(FilmNotFoundException exception) {
        log.error(exception.getMessage());
        return ErrorResponse.builder().errorCode(HttpStatus.NOT_FOUND.value()).description(exception.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUncaught(Exception exception) {
        log.error(exception.getMessage());
        return ErrorResponse.builder()
                .errorCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).description(exception.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExistException(UserAlreadyExistException exception) {
        log.error(exception.getMessage());
        return ErrorResponse.builder().errorCode(HttpStatus.CONFLICT.value()).description(exception.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleFilmAlreadyExistException(FilmAlreadyExistException exception) {
        log.error(exception.getMessage());
        return ErrorResponse.builder().errorCode(HttpStatus.CONFLICT.value()).description(exception.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidDurationException(InvalidDurationException exception) {
        log.error(exception.getMessage());
        return ErrorResponse.builder()
                .errorCode(HttpStatus.BAD_REQUEST.value()).description(exception.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidReleaseDateException(InvalidReleaseDateException exception) {
        log.error(exception.getMessage());
        return ErrorResponse.builder()
                .errorCode(HttpStatus.BAD_REQUEST.value()).description(exception.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(ValidationException exception) {
        log.error(exception.getMessage());
        return ErrorResponse.builder()
                .errorCode(HttpStatus.BAD_REQUEST.value()).description(exception.getMessage()).build();
    }
}