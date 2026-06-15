package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.gjennomfor

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class GjennomførIndeksreguleringBidragScheduler(
    private val gjennomførIndeksreguleringBidragBatch: GjennomførIndeksreguleringBidragBatch,
) {
    @Scheduled(cron = $$"${GJENNOMFOR_INDEKSREGULERING_BIDRAG_CRON:-}")
    @SchedulerLock(name = "gjennomforIndeksreguleringBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av gjennomfør indeksregulering bidrag batch" }
        gjennomførIndeksreguleringBidragBatch.start()
    }
}
