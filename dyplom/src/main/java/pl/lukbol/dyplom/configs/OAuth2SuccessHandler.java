package pl.lukbol.dyplom.configs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.services.OAuth2CodeService;
import pl.lukbol.dyplom.utilities.JwtUtil;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    @Value("${app.oauth2.redirect-url}")
    private String redirectUrl;

    private final OAuth2CodeService codeService;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        String email = authentication.getName();
        String code = codeService.createCode(email);

        String separator = redirectUrl.contains("?") ? "&" : "?";
        response.sendRedirect(redirectUrl + separator + "code=" + code);
    }
}
