package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak

import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class FatteVedtakRevurderForskuddBatchProcessor(
    val revurderForskuddService: RevurderForskuddService,
) : ItemProcessor<RevurderingForskudd, Unit> {
    private var simuler: Boolean = true

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
    }

    override fun process(revurderingForskudd: RevurderingForskudd) {
        revurderForskuddService.fattVedtakOmRevurderingForskudd(revurderingForskudd, simuler)
    }
}
