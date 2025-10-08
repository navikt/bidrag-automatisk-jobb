package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.combinedLogger
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultat
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
    private val aldersjusteringRepository: AldersjusteringRepository,
) : ItemProcessor<Aldersjustering, AldersjusteringResultat> {
    private var simuler: Boolean = true

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
    }

    override fun process(aldersjustering: Aldersjustering) =
        try {
            aldersjusteringService.utførAldersjustering(
                aldersjustering = aldersjustering,
                stønadstype = Stønadstype.BIDRAG,
                simuler,
            )
        } catch (e: Exception) {
            // Utfør en ekstra feilhåndtering her hvis feilhåndtering i aldersjustering feiler (pga det forsøkes å opprette vedtaksforslag)
            aldersjustering.status = Status.FEILET
            aldersjustering.behandlingstype = Behandlingstype.FEILET
            aldersjustering.begrunnelse = listOf(e.message ?: "Ukjent feil")
            aldersjusteringRepository.save(aldersjustering)

            combinedLogger.warn(e) {
                "Det skjedde en feil ved beregning av aldersjustering ${aldersjustering.id} " +
                    "for barn ${aldersjustering.barn.tilStønadsid(aldersjustering.stønadstype)}"
            }
            null
        }
}
