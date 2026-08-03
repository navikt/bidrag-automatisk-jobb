package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.brevbputland

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class BrevBpUtlandIndeksreguleringBidragScheduler(
    private val brevBpUtlandIndeksreguleringBidragBatch: BrevBpUtlandIndeksreguleringBidragBatch,
) {
    @Scheduled(cron = $$"${INDEKSREGULERING_BIDRAG_BREV_BP_UTLAND_CRON:-}")
    @SchedulerLock(name = "brevBpUtlandIndeksreguleringBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av brev BP utland indeksregulering bidrag batch" }
        brevBpUtlandIndeksreguleringBidragBatch.start()
    }
}
