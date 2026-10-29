package pl.lukbol.dyplom.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.lukbol.dyplom.DTOs.exception.ErrorMessageDTO;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_MSG = "Error message: ";

    @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ErrorMessageDTO> handleAuthExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorMessageDTO> handleDatabaseException(DataAccessException ex) {
        log.error("Blad dostepu do bazy danych: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessageDTO(ERROR_MSG, "Database error: " + ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.UserWithEmailAlreadyExistsException.class)
    public ResponseEntity<ErrorMessageDTO> handleUserAlreadyExists(ApplicationException.UserWithEmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.PasswordsMismatchException.class)
    public ResponseEntity<ErrorMessageDTO> handlePasswordsMismatch(ApplicationException.PasswordsMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.UserNotFoundException.class)
    public ResponseEntity<ErrorMessageDTO> handleUserNotFound(ApplicationException.UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.ParticipantsListIsEmptyException.class)
    public ResponseEntity<ErrorMessageDTO> handleParticipantsEmpty(ApplicationException.ParticipantsListIsEmptyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.ConversationNotFoundException.class)
    public ResponseEntity<ErrorMessageDTO> handleConversationNotFound(ApplicationException.ConversationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.LastMessageNotFoundException.class)
    public ResponseEntity<ErrorMessageDTO> handleLastMessageNotFound(ApplicationException.LastMessageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.InvalidDateException.class)
    public ResponseEntity<ErrorMessageDTO> handleInvalidDate(ApplicationException.InvalidDateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.OrderNotFoundException.class)
    public ResponseEntity<ErrorMessageDTO> handleOrderNotFound(ApplicationException.OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.MaterialNotFoundException.class)
    public ResponseEntity<ErrorMessageDTO> handleMaterialNotFound(ApplicationException.MaterialNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.PriceNotFoundException.class)
    public ResponseEntity<ErrorMessageDTO> handlePriceNotFound(ApplicationException.PriceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessageDTO(ERROR_MSG, ex.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessageDTO> handleAllOtherExceptions(
            Exception ex) {

        log.error("Unhandled application exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessageDTO(
                        ERROR_MSG,
                        "An unexpected server error occurred"
                ));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessageDTO> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorMessageDTO(ERROR_MSG, errors));
    }
    @ExceptionHandler(ApplicationException.InvalidOAuth2CodeException.class)
    public ResponseEntity<ErrorMessageDTO> handleInvalidOAuth2Code(
            ApplicationException.InvalidOAuth2CodeException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorMessageDTO(
                        ERROR_MSG,
                        "Invalid or expired authorization code"
                ));
    }
    @ExceptionHandler(
            ApplicationException.InvalidResetTokenException.class
    )
    public ResponseEntity<ErrorMessageDTO> handleInvalidResetToken() {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessageDTO(
                        ERROR_MSG,
                        "Invalid or expired password reset token"
                ));
    }

}