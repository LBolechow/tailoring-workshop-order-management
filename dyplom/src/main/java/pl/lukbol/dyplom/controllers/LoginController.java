package pl.lukbol.dyplom.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.lukbol.dyplom.DTOs.auth.AuthResponseDTO;
import pl.lukbol.dyplom.DTOs.auth.LoginRequestDTO;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.configs.CustomUserDetailsService;
import pl.lukbol.dyplom.services.OAuth2CodeService;
import pl.lukbol.dyplom.utilities.JwtUtil;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    private final OAuth2CodeService codeService;

    public record OAuth2ExchangeRequest(
            @NotBlank(message = Messages.VALIDATION_OAUTH_CODE_REQUIRED)
            @Size(max = 256, message = Messages.VALIDATION_OAUTH_CODE_TOO_LONG)
            String code
    ) {}

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.email());
        String token = jwtUtil.generateToken(userDetails.getUsername());
        log.info("Zalogowano uzytkownika: {}", loginRequest.email());

        return ResponseEntity.ok(new AuthResponseDTO(token, Messages.LOGIN_SUCCESS));
    }
    @PostMapping("/oauth2/exchange")
    public ResponseEntity<AuthResponseDTO> exchange(
            @Valid @RequestBody OAuth2ExchangeRequest request) {

        String email = codeService.consumeCode(request.code());
        String token = jwtUtil.generateToken(email);

        return ResponseEntity.ok(
                new AuthResponseDTO(token, Messages.LOGIN_SUCCESS)
        );
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO> logout(HttpServletRequest request) {
        String token = jwtUtil.extractJwtFromRequest(request);
        if (token == null) {
            throw new BadCredentialsException(Messages.TOKEN_INVALID_OR_EXPIRED);
        }

        jwtUtil.blacklistToken(token);
        log.info("Wylogowanie - token dodany do blacklisty");

        return ResponseEntity.ok(new ApiResponseDTO(Messages.LOGOUT_SUCCESS));
    }
}