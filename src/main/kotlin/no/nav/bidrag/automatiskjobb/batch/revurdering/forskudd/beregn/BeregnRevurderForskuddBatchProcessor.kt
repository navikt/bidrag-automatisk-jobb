package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.beregn

import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class BeregnRevurderForskuddBatchProcessor(
    private val revurderForskuddService: RevurderForskuddService,
) : ItemProcessor<RevurderingForskudd, Unit> {
    private var simuler: Boolean = true
    private var antallMånederForBeregning: Long = 3

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler")!!.toBoolean()
        antallMånederForBeregning = stepExecution.jobParameters.getString("antallManederForBeregning")!!.toLong()
    }

    override fun process(item: RevurderingForskudd) {
        revurderForskuddService.beregnRevurderForskudd(item, simuler, antallMånederForBeregning)
    }
}
