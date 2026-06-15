package no.nav.bidrag.automatiskjobb.batch.indeksregulering.forskudd.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class OpprettIndeksreguleringForskuddScheduler(
    private val opprettIndeksreguleringForskuddBatch: OpprettIndeksreguleringForskuddBatch,
) {
    @Scheduled(cron = $$"${INDEKSREGULERING_FORSKUDD_OPPRETT_CRON:-}")
    @SchedulerLock(name = "opprettIndeksreguleringForskudd", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av opprett indeksregulering forskudd batch" }
        opprettIndeksreguleringForskuddBatch.start()
    }
}
