package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@StepScope
class FattVedtakOmAldersjusteringerBidragBatchProcessor(
    private val aldersjusteringService: AldersjusteringService,
) : ItemProcessor<Aldersjustering, Unit> {
    private var simuler: Boolean = true
    private var kunRedusertBidrag: Boolean = false

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
        kunRedusertBidrag = stepExecution.jobParameters.getString("kunRedusertBidrag").toBoolean()
    }

    override fun process(aldersjustering: Aldersjustering): Unit? =
        try {
            if (kunRedusertBidrag && !aldersjusteringService.erBidragRedusert(aldersjustering)) {
                null
            } else {
                aldersjusteringService.fattVedtakOmAldersjustering(aldersjustering, simuler)
            }
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved fatting av vedtak for aldersjustering ${aldersjustering.id}" }
            null
        }
}
