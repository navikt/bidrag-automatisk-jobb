package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

@Component
class RevurderingslenkeRevurderForskuddScheduler(
    private val revurderingslenkeRevurderForskuddBatch: RevurderingslenkeRevurderForskuddBatch,
) {
    @Scheduled(cron = $$"${REVURDER_FORSKUDD_REVURDERINGSLENKE_CRON:-}")
    @SchedulerLock(name = "revurderingslenkeRevurderForskudd", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av revurderingslenke revurder forskudd batch" }
        revurderingslenkeRevurderForskuddBatch.start(
            søktFraDato = LocalDate.now().minusMonths(12),
            forMåned = null,
        )
    }
}
