package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd.EvaluerRevurderForskuddService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger { }

@Component
class EvaluerRevurderForskuddBatchProcessor(
    private val evaluerRevurderForskuddService: EvaluerRevurderForskuddService,
) : ItemProcessor<RevurderingForskudd, RevurderingForskudd> {
    private var simuler: Boolean = true
    private var antallMånederForBeregning: Long = 3
    private var beregnFraMåned: YearMonth = YearMonth.now().plusMonths(1)

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        stepExecution.jobParameters
            .getString("simuler")
            ?.toBoolean()
            ?.let { simuler = it }
        stepExecution.jobParameters
            .getString("antallManederForBeregning")
            ?.toLong()
            ?.let { antallMånederForBeregning = it }
        stepExecution.jobParameters.getString("beregnFraManed")?.let { beregnFraMåned = YearMonth.parse(it) }
    }

    override fun process(item: RevurderingForskudd): RevurderingForskudd? {
        if (item.status != Status.UBEHANDLET) {
            LOGGER.info { "Revurdering forskudd ${item.id} er allerede ${item.status}. Skal ikke evalueres på nytt." }
            return null
        }
        return evaluerRevurderForskuddService.evaluerRevurderForskudd(
            item,
            simuler,
            antallMånederForBeregning,
            beregnFraMåned,
        )
    }
}
