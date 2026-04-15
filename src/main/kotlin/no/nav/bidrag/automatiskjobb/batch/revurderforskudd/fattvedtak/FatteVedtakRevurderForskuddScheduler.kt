package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class FatteVedtakRevurderForskuddScheduler(
    private val fatteVedtakRevurderForskuddBatch: FatteVedtakRevurderForskuddBatch,
) {
    @Scheduled(cron = $$"${REVURDER_FORSKUDD_FATTE_VEDTAK_CRON:-}")
    @SchedulerLock(name = "fatteVedtakRevurderForskudd", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av fatte vedtak revurder forskudd batch" }
        fatteVedtakRevurderForskuddBatch.start(simuler = false)
    }
}
