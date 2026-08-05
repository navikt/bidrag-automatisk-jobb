package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.fattvedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class FattVedtakIndeksreguleringBidragScheduler(
    private val fattVedtakIndeksreguleringBidragBatch: FattVedtakIndeksreguleringBidragBatch,
) {
    @Scheduled(cron = $$"${INDEKSREGULERING_BIDRAG_FATT_VEDTAK_CRON:-}")
    @SchedulerLock(name = "fattVedtakIndeksreguleringBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av fatt vedtak indeksregulering bidrag batch" }
        fattVedtakIndeksreguleringBidragBatch.start(simuler = false)
    }
}
