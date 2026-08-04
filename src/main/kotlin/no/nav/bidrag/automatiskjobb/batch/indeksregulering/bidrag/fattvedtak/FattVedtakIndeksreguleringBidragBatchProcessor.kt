package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.fattvedtak

import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.FattVedtakIndeksreguleringBidragService
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
@StepScope
class FattVedtakIndeksreguleringBidragBatchProcessor(
    private val fattVedtakIndeksreguleringBidragService: FattVedtakIndeksreguleringBidragService,
) : ItemProcessor<Indeksregulering, Unit> {
    private var simuler: Boolean = true

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
    }

    override fun process(indeksregulering: Indeksregulering) {
        fattVedtakIndeksreguleringBidragService.fattVedtak(indeksregulering, simuler)
    }
}
