package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
@StepScope
class FattVedtakOmAldersjusteringerBidragBatchProcessor(
    private val aldersjusteringService: AldersjusteringService,
) : ItemProcessor<Aldersjustering, Unit> {
    private var simuler: Boolean = true

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
    }

    override fun process(aldersjustering: Aldersjustering): Unit? =
        aldersjusteringService.fattVedtakOmAldersjustering(aldersjustering, simuler)
}
