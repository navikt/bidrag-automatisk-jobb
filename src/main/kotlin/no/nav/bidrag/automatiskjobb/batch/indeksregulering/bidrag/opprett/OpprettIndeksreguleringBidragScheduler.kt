package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class OpprettIndeksreguleringBidragScheduler(
    private val opprettIndeksreguleringBidragBatch: OpprettIndeksreguleringBidragBatch,
) {
    @Scheduled(cron = $$"${INDEKSREGULERING_BIDRAG_OPPRETT_CRON:-}")
    @SchedulerLock(name = "opprettIndeksreguleringBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av opprett indeksregulering bidrag batch" }
        opprettIndeksreguleringBidragBatch.start()
    }
}
