package pl.lukbol.dyplom.unitTests;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import pl.lukbol.dyplom.configs.OAuth2SuccessHandler;
import pl.lukbol.dyplom.services.OAuth2CodeService;
import pl.lukbol.dyplom.utilities.JwtUtil;
import static org.mockito.Mockito.*;

class OAuth2SuccessHandlerTest {
    @Test void redirectsWithSingleUseCodeRatherThanJwt() throws Exception {
        OAuth2CodeService codes = mock(OAuth2CodeService.class);
        JwtUtil jwt = mock(JwtUtil.class);
        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(codes, jwt);
        ReflectionTestUtils.setField(handler, "redirectUrl", "http://localhost:3000/oauth-success");
        Authentication authentication = mock(Authentication.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(authentication.getName()).thenReturn("user@example.com");
        when(codes.createCode("user@example.com")).thenReturn("one-time-code");
        handler.onAuthenticationSuccess(mock(HttpServletRequest.class), response, authentication);
        verify(response).sendRedirect("http://localhost:3000/oauth-success?code=one-time-code");
        verifyNoInteractions(jwt);
    }
}
