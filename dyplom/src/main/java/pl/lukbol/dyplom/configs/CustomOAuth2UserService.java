package pl.lukbol.dyplom.configs;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.classes.Privilege;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.common.UserRole;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.security.SecureRandom;
import java.util.*;

@Component
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String EMAIL_ATTR = "email";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    private final SecureRandom random = new SecureRandom();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = delegate.loadUser(userRequest);

        String email = oauthUser.getAttribute(EMAIL_ATTR);
        if (email == null) {
            throw new OAuth2AuthenticationException(Messages.OAUTH_EMAIL_NOT_FOUND);
        }

        User user = userRepository.findOptionalByEmail(email)
                .orElseGet(() -> createNewUser(email, oauthUser));

        Collection<GrantedAuthority> authorities = getAuthorities(user.getRole());

        return new DefaultOAuth2User(authorities, oauthUser.getAttributes(), EMAIL_ATTR);
    }

    private User createNewUser(String email, OAuth2User oauthUser) {
        String name = oauthUser.getAttribute("name");
        String generatedPassword = generateRandomPassword();

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name != null ? name : email.split("@")[0]);
        newUser.setPassword(passwordEncoder.encode(generatedPassword));
        newUser.setRole(roleRepository.findByName(UserRole.CLIENT.authority()));

        return userRepository.save(newUser);
    }

    private Collection<GrantedAuthority> getAuthorities(Role role) {
        if (role == null) {
            return Collections.emptyList();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role.getName()));

        if (role.getPrivileges() != null) {
            for (Privilege privilege : role.getPrivileges()) {
                authorities.add(new SimpleGrantedAuthority(privilege.getName()));
            }
        }

        return authorities;
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
