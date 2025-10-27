
package JK.pfm.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.stream.Collectors;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler that centralizes API error responses across the application.
 *
 * <p>Converts common exceptions into a simple JSON error format using an internal
 * {@code ApiError} record that includes the following fields:
 * <ul>
 *   <li><b>message</b> – short human-readable description of the error</li>
 *   <li><b>path</b> – request URI where the error occurred</li>
 *   <li><b>timestamp</b> – time when the error was generated</li>
 * </ul>
 *
 * <p>Examples of responses:
 * <pre>
 * {
 *   "message": "Username already taken",
 *   "path": "/api/users/register",
 *   "timestamp": "2025-10-27T08:30:00"
 * }
 * </pre>
 *
 * <p>This handler ensures consistent error formatting for all controllers and prevents
 * stack traces or raw exceptions from leaking to the client.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  record ApiError(String message, String path, LocalDateTime timestamp) {}

    /**
   * Handles bean-validation errors thrown by {@code @Valid} annotated parameters or DTOs.
   * Aggregates field-level messages into a single semicolon-separated string.
   *
   * @param ex  the validation exception containing field errors
   * @param req the current HTTP request
   * @return a {@link ResponseEntity} with HTTP 400 (Bad Request) and aggregated validation messages
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                   HttpServletRequest req) {
    String msg = ex.getFieldErrors().stream()
                   .map(FieldError::getDefaultMessage)
                   .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest()
        .body(new ApiError(msg, req.getRequestURI(), LocalDateTime.now()));
  }

  /**
   * Handles {@link ResponseStatusException} thrown manually from services or controllers.
   * The HTTP status and reason are preserved exactly as provided.
   *
   * <p>Example: when a service throws
   * {@code new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken")},
   * the client receives a 409 response with that message.
   *
   * @param ex  the status exception
   * @param req the current HTTP request
   * @return a {@link ResponseEntity} with the same status and message
   */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleStatusExc(ResponseStatusException ex,
                                                  HttpServletRequest req) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(new ApiError(ex.getReason(),
                           req.getRequestURI(),
                           LocalDateTime.now()));
  }

  /**
   * Handles all unanticipated exceptions that are not explicitly mapped elsewhere.
   * Prints the stack trace to server logs (for debugging) and returns a generic
   * 500 Internal Server Error message to the client.
   *
   * @param ex  the uncaught exception
   * @param req the current HTTP request
   * @return a {@link ResponseEntity} with HTTP 500 and a generic message
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleAll(Exception ex, HttpServletRequest req) {
    ex.printStackTrace();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError("An unexpected error occurred",
                           req.getRequestURI(),
                           LocalDateTime.now()));
  }
  
  /**
   * Handles optimistic-locking failures that can occur when multiple clients
   * attempt to update the same database row simultaneously.
   *
   * @param ex  the optimistic-locking exception
   * @param req the current HTTP request
   * @return a {@link ResponseEntity} with HTTP 409 (Conflict) and an informative message
   */
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiError> handleOptimisticLock(
          ObjectOptimisticLockingFailureException ex,
          HttpServletRequest req) {
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(new ApiError("Resource was updated elsewhere; please retry",
                         req.getRequestURI(),
                         LocalDateTime.now()));
  }
}

