package com.dental.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> api(ApiException e) {
    return ResponseEntity.status(e.getStatus())
        .body(ApiError.of(e.getStatus().value(), e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
    var errors = new LinkedHashMap<String, String>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ApiError(Instant.now(), 400, "Invalid request", errors));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiError> malformed(Exception e) {
    return ResponseEntity.badRequest()
        .body(ApiError.of(400, "Invalid JSON, field or parameter format"));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    CannotAcquireLockException.class
  })
  public ResponseEntity<ApiError> conflict(Exception e) {
    return ResponseEntity.status(409)
        .body(ApiError.of(409, "Data conflict or overlapping appointment; refresh and retry"));
  }
}
