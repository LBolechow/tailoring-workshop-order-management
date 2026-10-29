package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.exceptions.GlobalExceptionHandler;
import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerSecurityTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void unexpectedFailureDoesNotRevealInternalDetails() {
        var response = handler.handleAllOtherExceptions(new IllegalStateException("db-password-secret"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().detail()).doesNotContain("db-password-secret");
        assertThat(response.getBody().detail()).isEqualTo("An unexpected server error occurred");
    }
    @Test void invalidResetTokenGetsGenericError() {
        var response = handler.handleInvalidResetToken();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().detail()).contains("Invalid or expired");
    }
    @Test void invalidOAuthCodeGetsGenericError() {
        var response = handler.handleInvalidOAuth2Code(new ApplicationException.InvalidOAuth2CodeException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().detail()).contains("Invalid or expired");
    }
}
