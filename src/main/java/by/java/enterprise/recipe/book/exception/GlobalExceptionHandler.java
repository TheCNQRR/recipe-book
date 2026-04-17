package by.java.enterprise.recipe.book.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse("VALIDATION_ERROR", ex.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(ProductUsedInDishException.class)
    public ResponseEntity<ErrorResponse> handleProductInUse(ProductUsedInDishException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse("PRODUCT_IN_USE", ex.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse("NOT_FOUND", ex.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Ошибка валидации");
        return ResponseEntity.badRequest().body(
                new ErrorResponse("VALIDATION_ERROR", message, Instant.now())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse("VALIDATION_ERROR", ex.getMessage(), Instant.now())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage(), Instant.now())
        );
    }

    public record ErrorResponse(String code, String message, Instant timestamp) {}
}
