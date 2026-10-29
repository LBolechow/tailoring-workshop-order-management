package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import pl.lukbol.dyplom.utilities.ResetTokenHasher;
import pl.lukbol.dyplom.utilities.GenerateCode;
import static org.assertj.core.api.Assertions.*;

class ResetTokenHasherTest {
    private final ResetTokenHasher hasher = new ResetTokenHasher();
    @Test void producesConsistentSha256HexAndNotPlaintext() {
        String token = "private-token";
        assertThat(hasher.hash(token)).hasSize(64).isEqualTo(hasher.hash(token)).doesNotContain(token);
        assertThat(hasher.hash(token)).isNotEqualTo(hasher.hash("other-token"));
    }
    @Test void generatedTokensAreUrlSafeAndDistinct() {
        String a = GenerateCode.generateResetToken();
        String b = GenerateCode.generateResetToken();
        assertThat(a).matches("[A-Za-z0-9_-]{43}");
        assertThat(b).isNotEqualTo(a);
    }
}
