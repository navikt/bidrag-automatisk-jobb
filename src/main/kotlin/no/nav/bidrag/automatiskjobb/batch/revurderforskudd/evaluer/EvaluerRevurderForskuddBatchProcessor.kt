package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd.EvaluerRevurderForskuddService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.time.YearMonth

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

    override fun process(item: RevurderingForskudd): RevurderingForskudd? =
        evaluerRevurderForskuddService.evaluerRevurderForskudd(item, simuler, antallMånederForBeregning, beregnFraMåned)
}
