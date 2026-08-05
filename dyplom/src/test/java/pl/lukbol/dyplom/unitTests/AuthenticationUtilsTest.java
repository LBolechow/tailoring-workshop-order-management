package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * checkmail() rozpoznaje cztery rozne typy principala, bo aplikacja obsluguje
 * logowanie lokalne (JWT) i przez Google (OAuth2/OIDC). Kazda sciezka
 * przechowuje email w innym miejscu.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationUtilsTest {

    @Mock
    private UserDetails userDetails;

    @Test
    void checkmail_shouldReturnUsername_forUserDetails() {
        when(userDetails.getUsername()).thenReturn("jan@test.pl");

        assertThat(AuthenticationUtils.checkmail(userDetails)).isEqualTo("jan@test.pl");
    }

    @Test
    void checkmail_shouldReturnEmailAttribute_forOidcUser() {
        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("sub", "12345", "email", "google@test.pl")
        );
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT")),
                idToken,
                "email"
        );

        assertThat(AuthenticationUtils.checkmail(oidcUser)).isEqualTo("google@test.pl");
    }

    @Test
    void checkmail_shouldReturnEmailAttribute_forOAuth2AuthenticationToken() {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT")),
                Map.of("email", "oauth@test.pl", "name", "Jan"),
                "email"
        );
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google"
        );

        assertThat(AuthenticationUtils.checkmail(token)).isEqualTo("oauth@test.pl");
    }

    @Test
    void checkmail_shouldReturnName_forUsernamePasswordAuthenticationToken() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("local@test.pl", "haslo");

        assertThat(AuthenticationUtils.checkmail(token)).isEqualTo("local@test.pl");
    }

    @Test
    void checkmail_shouldThrowUserNotFoundException_forUnsupportedPrincipal() {
        assertThatThrownBy(() -> AuthenticationUtils.checkmail("zwykly string"))
                .isInstanceOf(ApplicationException.UserNotFoundException.class)
                .hasMessage(Messages.USER_NOT_FOUND_BY_EMAIL);
    }

    @Test
    void checkmail_shouldThrowUserNotFoundException_forNullPrincipal() {
        assertThatThrownBy(() -> AuthenticationUtils.checkmail(null))
                .isInstanceOf(ApplicationException.UserNotFoundException.class)
                .hasMessage(Messages.USER_NOT_FOUND_BY_EMAIL);
    }
}
