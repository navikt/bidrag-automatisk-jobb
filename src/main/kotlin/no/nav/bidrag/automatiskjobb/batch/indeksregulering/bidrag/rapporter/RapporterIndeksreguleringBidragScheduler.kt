package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class RapporterIndeksreguleringBidragScheduler(
    private val rapporterIndeksreguleringBidragBatch: RapporterIndeksreguleringBidragBatch,
) {
    @Scheduled(cron = $$"${INDEKSREGULERING_BIDRAG_RAPPORTER_CRON:-}")
    @SchedulerLock(name = "rapporterIndeksreguleringBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av rapporter indeksregulering bidrag batch" }
        rapporterIndeksreguleringBidragBatch.start()
    }
}
