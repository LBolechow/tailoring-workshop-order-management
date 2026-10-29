package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.Test;
import pl.lukbol.dyplom.common.SecurityPaths;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.*;

class SecurityPathsTest {
    @Test void publicResetAndExchangePathsAreAllowedAndSkipJwtFilter() {
        SecurityPaths paths = new SecurityPaths();
        for (String route : new String[]{"/forgot-password", "/reset-password", "/api/auth/oauth2/exchange"}) {
            assertThat(SecurityPaths.skipFilterUrls).contains(route);
            assertThat(Arrays.asList(paths.PERMIT_ALL_PATHS)).contains(route);
        }
    }
    @Test void legacyResetRouteIsNotPublic() {
        assertThat(SecurityPaths.skipFilterUrls).doesNotContain("/send-new-password");
        assertThat(Arrays.asList(new SecurityPaths().PERMIT_ALL_PATHS)).doesNotContain("/send-new-password");
    }
}
