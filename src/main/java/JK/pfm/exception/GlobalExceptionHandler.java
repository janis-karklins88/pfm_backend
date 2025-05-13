
package JK.pfm.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.stream.Collectors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

  record ApiError(String message, String path, LocalDateTime timestamp) {}

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                   HttpServletRequest req) {
    String msg = ex.getFieldErrors().stream()
                   .map(FieldError::getDefaultMessage)
                   .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest()
        .body(new ApiError(msg, req.getRequestURI(), LocalDateTime.now()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleStatusExc(ResponseStatusException ex,
                                                  HttpServletRequest req) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(new ApiError(ex.getReason(),
                           req.getRequestURI(),
                           LocalDateTime.now()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleAll(Exception ex, HttpServletRequest req) {
    ex.printStackTrace();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError("An unexpected error occurred",
                           req.getRequestURI(),
                           LocalDateTime.now()));
  }
}

