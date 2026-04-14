package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class EvaluerRevurderForskuddScheduler(
    private val evaluerRevurderForskuddBatch: EvaluerRevurderForskuddBatch,
) {
    @Scheduled(cron = $$"${REVURDER_FORSKUDD_EVALUER_CRON:-}")
    @SchedulerLock(name = "evaluerRevurderForskudd", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av evaluer revurder forskudd batch" }
        evaluerRevurderForskuddBatch.start(simuler = false, beregnFraMåned = null, forMåned = null)
    }
}


