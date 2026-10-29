package com.example.taskmanager.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(new ApiError(LocalDateTime.now(), 400, "Los datos enviados no son válidos", errors));
    }
    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<ApiError> notFound(TaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(LocalDateTime.now(), 404, ex.getMessage(), List.of()));
    }
    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ApiError> productNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(LocalDateTime.now(), 404, ex.getMessage(), List.of()));
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> resourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(LocalDateTime.now(), 404, ex.getMessage(), List.of()));
    }
    @ExceptionHandler(DuplicateSkuException.class)
    ResponseEntity<ApiError> duplicateSku(DuplicateSkuException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(LocalDateTime.now(), 409, ex.getMessage(), List.of()));
    }
    @ExceptionHandler(ReportGenerationException.class)
    ResponseEntity<ApiError> reportGeneration(ReportGenerationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError(LocalDateTime.now(), 500, ex.getMessage(), List.of()));
    }
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    ResponseEntity<ApiError> authentication(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiError(LocalDateTime.now(), 401, ex.getMessage(), List.of()));
    }
    @ExceptionHandler({DuplicateUsernameException.class, AdminSetupCompleteException.class})
    ResponseEntity<ApiError> accountConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError(LocalDateTime.now(), 409, ex.getMessage(), List.of()));
    }
    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ApiError> accountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(LocalDateTime.now(), 404, ex.getMessage(), List.of()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiError(LocalDateTime.now(), 400, ex.getMessage(), List.of()));
    }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> illegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(LocalDateTime.now(), 409, ex.getMessage(), List.of()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "DELETE".equals(request.getMethod())
                && request.getRequestURI().matches("/products/\\d+")
            ? "No se puede eliminar este producto porque está asociado a operaciones comerciales existentes. El producto se conservó para proteger el historial."
            : "La operación entra en conflicto con datos existentes";
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError(LocalDateTime.now(), 409, message, List.of()));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> general(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(500).body(new ApiError(LocalDateTime.now(), 500, "Ocurrió un error interno", List.of()));
    }
}
