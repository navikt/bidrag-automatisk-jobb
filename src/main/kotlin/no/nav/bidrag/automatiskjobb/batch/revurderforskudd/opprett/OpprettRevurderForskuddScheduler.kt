package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class OpprettRevurderForskuddScheduler(
    private val opprettRevurderForskuddBatch: OpprettRevurderForskuddBatch,
) {
    @Scheduled(cron = $$"${REVURDER_FORSKUDD_OPPRETT_CRON:-}")
    @SchedulerLock(name = "opprettRevurderForskudd", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av opprett revurder forskudd batch" }
        opprettRevurderForskuddBatch.start()
    }
}
