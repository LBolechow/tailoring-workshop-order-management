package pl.lukbol.dyplom.services;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.lukbol.dyplom.exceptions.ApplicationException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuth2CodeService {

    private record Entry(String email, Instant expiresAt) {}

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Entry> codes =
            new ConcurrentHashMap<>();

    public String createCode(String email) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        String code = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        codes.put(code, new Entry(
                email,
                Instant.now().plusSeconds(60)
        ));

        return code;
    }

    public String consumeCode(String code) {
        Entry entry = codes.remove(code);

        if (entry == null ||
                !Instant.now().isBefore(entry.expiresAt())) {
            throw new ApplicationException.InvalidOAuth2CodeException();
        }

        return entry.email();
    }
}
