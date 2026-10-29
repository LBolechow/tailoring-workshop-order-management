
package pl.lukbol.dyplom.utilities;

import java.security.SecureRandom;
import java.util.Base64;

public final class GenerateCode {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private GenerateCode() {}

    public static String generateResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
