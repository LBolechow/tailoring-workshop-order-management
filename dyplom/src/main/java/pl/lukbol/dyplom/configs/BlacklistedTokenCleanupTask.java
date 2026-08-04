package pl.lukbol.dyplom.configs;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.repositories.BlacklistedTokenRepository;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class BlacklistedTokenCleanupTask {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void removeExpiredTokens() {
        blacklistedTokenRepository.deleteByExpiresAtBefore(new Date());
    }
}