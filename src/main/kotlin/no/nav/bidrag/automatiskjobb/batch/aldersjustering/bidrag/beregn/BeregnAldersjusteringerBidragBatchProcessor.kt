package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@StepScope
class BeregnAldersjusteringerBidragBatchProcessor(
    private val aldersjusteringService: AldersjusteringService,
) : ItemProcessor<Aldersjustering, Unit> {
    private var simuler: Boolean = true

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
    }

    override fun process(aldersjustering: Aldersjustering) {
        try {
            aldersjusteringService.utførAldersjustering(
                aldersjustering = aldersjustering,
                stønadstype = Stønadstype.BIDRAG,
                simuler,
            )
        } catch (e: Exception) {
            log.warn(e) { "Det skjedde en feil ved beregning av aldersjustering ${aldersjustering.id}" }
        }
    }
}
