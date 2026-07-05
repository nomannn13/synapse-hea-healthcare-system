package com.synapse.hea.common.exception;

import com.synapse.hea.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(
    GlobalExceptionHandler.class
  );

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(
    MethodArgumentNotValidException ex,
    HttpServletRequest request
  ) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getBindingResult()
      .getFieldErrors()
      .forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
    return build(
      HttpStatus.BAD_REQUEST,
      "VALIDATION_ERROR",
      "Request validation failed",
      request,
      fields
    );
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> constraint(
    ConstraintViolationException ex,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.BAD_REQUEST,
      "VALIDATION_ERROR",
      ex.getMessage(),
      request,
      Map.of()
    );
  }

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ApiError> notFound(
    NotFoundException ex,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.NOT_FOUND,
      "NOT_FOUND",
      ex.getMessage(),
      request,
      Map.of()
    );
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<ApiError> conflict(
    ConflictException ex,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.CONFLICT,
      "CONFLICT",
      ex.getMessage(),
      request,
      Map.of()
    );
  }

  @ExceptionHandler({
    ForbiddenOperationException.class,
    AccessDeniedException.class,
  })
  ResponseEntity<ApiError> forbidden(Exception ex, HttpServletRequest request) {
    return build(
      HttpStatus.FORBIDDEN,
      "FORBIDDEN",
      ex.getMessage(),
      request,
      Map.of()
    );
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ApiError> credentials(
    BadCredentialsException ex,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.UNAUTHORIZED,
      "INVALID_CREDENTIALS",
      "Email or password is incorrect",
      request,
      Map.of()
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> integrity(
    DataIntegrityViolationException ex,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.CONFLICT,
      "DATA_CONFLICT",
      "The operation conflicts with existing data",
      request,
      Map.of()
    );
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(
    Exception ex,
    HttpServletRequest request
  ) {
    log.error("Unhandled exception", ex);
    return build(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "INTERNAL_ERROR",
      "An unexpected error occurred",
      request,
      Map.of()
    );
  }

  private ResponseEntity<ApiError> build(
    HttpStatus status,
    String code,
    String message,
    HttpServletRequest request,
    Map<String, String> fields
  ) {
    return ResponseEntity.status(status).body(
      new ApiError(
        Instant.now(),
        status.value(),
        code,
        message,
        request.getRequestURI(),
        fields
      )
    );
  }
}
