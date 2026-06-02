package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.reset

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultat
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
@StepScope
class ResetAldersjusteringerBidragBatchProcessor(
    private val aldersjusteringService: AldersjusteringService,
    private val aldersjusteringRepository: AldersjusteringRepository,
) : ItemProcessor<Aldersjustering, Boolean> {
    override fun process(aldersjustering: Aldersjustering) =
        try {
            aldersjusteringService.resetAldersjustering(
                aldersjustering = aldersjustering,
            )
            true
        } catch (e: Exception) {
            aldersjusteringRepository.save(aldersjustering)

            LOGGER.warn(e) {
                "Det skjedde en feil ved beregning av aldersjustering ${aldersjustering.id} " +
                    "for barn ${aldersjustering.barn.tilStønadsid(aldersjustering.stønadstype)}"
            }
            false
        }
}
