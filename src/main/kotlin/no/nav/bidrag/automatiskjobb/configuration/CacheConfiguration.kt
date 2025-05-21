package no.nav.bidrag.automatiskjobb.configuration

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.cache.EnableUserCache
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Duration

@Configuration
@EnableCaching
@Profile(value = ["!test"]) // Ignore cache on tests
@EnableUserCache
class CacheConfiguration {
    companion object {
        const val PERSON_CACHE = "PERSON_CACHE"
        const val PERSON_FØDSELSDATO_CACHE = "PERSON_FODSELSDATO_CACHE"
        const val PERSON_HUSTANDSMEDLEMMER_CACHE = "PERSON_HUSTANDSMEDLEMMER_CACHE"
        const val VEDTAK_CACHE = "VEDTAK_CACHE"
        const val SAK_CACHE = "SAK_CACHE"
        const val SAMHANDLER_CACHE = "SAMHANDLER_CACHE"
        const val SAKER_PERSON_CACHE = "SAKER_PERSON_CACHE"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(
            PERSON_FØDSELSDATO_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            SAMHANDLER_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            PERSON_HUSTANDSMEDLEMMER_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            SAKER_PERSON_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            PERSON_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            VEDTAK_CACHE,
            Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build(),
        )
        caffeineCacheManager.registerCustomCache(
            SAK_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        return caffeineCacheManager
    }
}
