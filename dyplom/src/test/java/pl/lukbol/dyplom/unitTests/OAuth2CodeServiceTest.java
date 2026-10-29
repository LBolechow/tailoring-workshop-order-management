package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import pl.lukbol.dyplom.services.OAuth2CodeService;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import static org.assertj.core.api.Assertions.*;

class OAuth2CodeServiceTest {
    private final OAuth2CodeService service = new OAuth2CodeService();

    @Test
    void codeIsOneTimeUse() {
        String code = service.createCode("jan@test.pl");
        assertThat(service.consumeCode(code)).isEqualTo("jan@test.pl");
        assertThatThrownBy(() -> service.consumeCode(code))
                .isInstanceOf(ApplicationException.InvalidOAuth2CodeException.class);
    }

    @Test
    void unknownCodeIsRejected() {
        assertThatThrownBy(() -> service.consumeCode("not-a-valid-code"))
                .isInstanceOf(ApplicationException.InvalidOAuth2CodeException.class);
    }
}
