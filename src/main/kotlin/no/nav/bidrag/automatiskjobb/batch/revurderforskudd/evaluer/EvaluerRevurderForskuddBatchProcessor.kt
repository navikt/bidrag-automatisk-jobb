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
) : ItemProcessor<RevurderingForskudd, Unit> {
    private var simuler: Boolean = true
    private var antallMånederForBeregning: Long = 3
    private var beregnFraMåned: YearMonth = YearMonth.now().minusYears(1)

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler")!!.toBoolean()
        antallMånederForBeregning = stepExecution.jobParameters.getString("antallManederForBeregning")!!.toLong()
        beregnFraMåned = YearMonth.parse(stepExecution.jobParameters.getString("beregnFraManed")!!)
    }

    override fun process(item: RevurderingForskudd) {
        evaluerRevurderForskuddService.evaluerRevurderForskudd(item, simuler, antallMånederForBeregning, beregnFraMåned)
    }
}
