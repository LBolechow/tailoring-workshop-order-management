package pl.lukbol.dyplom.utilities;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;

public class AuthenticationUtils {

    private static final String OAUTH_ATTR = "email";

    public static String checkmail(Object authentication) {
        if (authentication instanceof DefaultOidcUser oauth2User) {
            return oauth2User.getAttribute(OAUTH_ATTR);
        } else if (authentication instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getPrincipal().getAttribute(OAUTH_ATTR);
        } else if (authentication instanceof UsernamePasswordAuthenticationToken oauthToken) {
            return oauthToken.getName();
        } else {
            throw new ApplicationException.UserNotFoundException(Messages.USER_NOT_FOUND_BY_EMAIL);
        }
    }
}
