package lk.smartroute.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<?> notFound(ResourceNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<?> conflict(ConflictException ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenActionException.class)
    ResponseEntity<?> forbidden(ForbiddenActionException ex) {
        return body(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> accessDenied(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> badRequest(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> dataIntegrity(DataIntegrityViolationException ex) {
        return body(HttpStatus.CONFLICT, "Request conflicts with existing data");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<?> constraintViolation(ConstraintViolationException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
            "timestamp", Instant.now(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "message", "Validation failed",
            "errors", errors
        ));
    }

    private ResponseEntity<?> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "timestamp", Instant.now(),
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "message", message
        ));
    }
}
