package pl.lukbol.dyplom.unitTests;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pl.lukbol.dyplom.classes.BlacklistedToken;
import pl.lukbol.dyplom.repositories.BlacklistedTokenRepository;
import pl.lukbol.dyplom.utilities.JwtUtil;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private static final String SECRET =
            "dGVzdFNlY3JldEtleUZvckp3dFRlc3RpbmdQdXJwb3Nlc09ubHkxMjM0NTY3ODkw";
    private static final long EXPIRATION = 3_600_000L;

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", EXPIRATION);
    }

    @Test
    void generateToken_shouldProduceTokenWithUsernameAsSubject() {
        String token = jwtUtil.generateToken("jan@test.pl");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("jan@test.pl");
    }

    @Test
    void generateToken_shouldSetExpirationInTheFuture() {
        String token = jwtUtil.generateToken("jan@test.pl");

        assertThat(jwtUtil.extractExpiration(token)).isAfter(new Date());
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    void extractUsername_shouldThrow_whenTokenSignedWithDifferentSecret() {
        String token = jwtUtil.generateToken("jan@test.pl");
        ReflectionTestUtils.setField(jwtUtil, "secretKey",
                "aW5ueVNlY3JldEtleUZvckp3dFRlc3RpbmdQdXJwb3Nlc09ubHkxMjM0NTY3");

        assertThatThrownBy(() -> jwtUtil.extractUsername(token))
                .isInstanceOf(Exception.class);
    }

    // walidacja

    @Test
    void validateToken_shouldReturnTrue_forMatchingUsername() {
        String token = jwtUtil.generateToken("jan@test.pl");

        assertThat(jwtUtil.validateToken(token, "jan@test.pl")).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalse_forDifferentUsername() {
        String token = jwtUtil.generateToken("jan@test.pl");

        assertThat(jwtUtil.validateToken(token, "anna@test.pl")).isFalse();
    }

    @Test
    void isTokenExpired_shouldThrowExpiredJwtException_forAlreadyExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", -1000L);
        String expired = jwtUtil.generateToken("jan@test.pl");

        assertThatThrownBy(() -> jwtUtil.isTokenExpired(expired))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    // nagłówek Authorization

    @Test
    void extractJwtFromRequest_shouldReturnTokenWithoutBearerPrefix() {
        when(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");

        assertThat(jwtUtil.extractJwtFromRequest(request)).isEqualTo("abc.def.ghi");
    }

    @Test
    void extractJwtFromRequest_shouldReturnNull_whenHeaderMissing() {
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThat(jwtUtil.extractJwtFromRequest(request)).isNull();
    }

    @Test
    void extractJwtFromRequest_shouldReturnNull_whenPrefixWrong() {
        when(request.getHeader("Authorization")).thenReturn("Basic abc.def.ghi");

        assertThat(jwtUtil.extractJwtFromRequest(request)).isNull();
    }

    // blacklista

    @Test
    void isTokenBlacklisted_shouldReturnTrue_whenTokenPresentInRepository() {
        when(blacklistedTokenRepository.findOptionalByToken("token"))
                .thenReturn(Optional.of(new BlacklistedToken("token", new Date())));

        assertThat(jwtUtil.isTokenBlacklisted("token")).isTrue();
    }

    @Test
    void isTokenBlacklisted_shouldReturnFalse_whenTokenAbsent() {
        when(blacklistedTokenRepository.findOptionalByToken("token")).thenReturn(Optional.empty());

        assertThat(jwtUtil.isTokenBlacklisted("token")).isFalse();
    }

    @Test
    void blacklistToken_shouldSaveTokenWithItsExpiryDate() {
        String token = jwtUtil.generateToken("jan@test.pl");
        when(blacklistedTokenRepository.findOptionalByToken(token)).thenReturn(Optional.empty());

        jwtUtil.blacklistToken(token);

        ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(captor.capture());

        BlacklistedToken saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getExpiresAt()).isEqualTo(jwtUtil.extractExpiration(token));
    }

    @Test
    void blacklistToken_shouldNotSaveDuplicate_whenTokenAlreadyBlacklisted() {
        String token = jwtUtil.generateToken("jan@test.pl");
        when(blacklistedTokenRepository.findOptionalByToken(token))
                .thenReturn(Optional.of(new BlacklistedToken(token, new Date())));

        jwtUtil.blacklistToken(token);

        verify(blacklistedTokenRepository, never()).save(any());
    }
}