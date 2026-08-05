package pl.lukbol.dyplom.configs;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.repositories.BlacklistedTokenRepository;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistedTokenCleanupTask {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void removeExpiredTokens() {
        long before = blacklistedTokenRepository.count();
        blacklistedTokenRepository.deleteByExpiresAtBefore(new Date());
        long removed = before - blacklistedTokenRepository.count();
        log.info("Czyszczenie blacklisty: usunieto {} wygaslych tokenow", removed);
    }
}