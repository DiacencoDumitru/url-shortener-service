package com.url_shortener.exceptions.handler;

import com.url_shortener.exceptions.IdempotencyKeyConflictException;
import com.url_shortener.exceptions.InvalidIdempotencyKeyException;
import com.url_shortener.exceptions.RateLimitExceededException;
import com.url_shortener.exceptions.UrlNotFoundException;
import com.url_shortener.exceptions.model.UrlShortenerError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class UrlShortenerExceptionHandler {

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<UrlShortenerError> handleUrlNotFound(UrlNotFoundException ex) {
        UrlShortenerError errorResponse = UrlShortenerError.builder()
                .timestamp(LocalDateTime.now())
                .code(HttpStatus.NOT_FOUND.value())
                .status(HttpStatus.NOT_FOUND.name())
                .errors(List.of(ex.getMessage()))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<UrlShortenerError> handleRateLimitExceeded(RateLimitExceededException ex) {
        UrlShortenerError errorResponse = UrlShortenerError.builder()
                .timestamp(LocalDateTime.now())
                .code(HttpStatus.TOO_MANY_REQUESTS.value())
                .status(HttpStatus.TOO_MANY_REQUESTS.name())
                .errors(List.of(ex.getMessage()))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public ResponseEntity<UrlShortenerError> handleIdempotencyConflict(IdempotencyKeyConflictException ex) {
        UrlShortenerError errorResponse = UrlShortenerError.builder()
                .timestamp(LocalDateTime.now())
                .code(HttpStatus.CONFLICT.value())
                .status(HttpStatus.CONFLICT.name())
                .errors(List.of(ex.getMessage()))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<UrlShortenerError> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException ex) {
        UrlShortenerError errorResponse = UrlShortenerError.builder()
                .timestamp(LocalDateTime.now())
                .code(HttpStatus.BAD_REQUEST.value())
                .status(HttpStatus.BAD_REQUEST.name())
                .errors(List.of(ex.getMessage()))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
