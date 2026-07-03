package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.gjennomfor

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.IndeksreguleringBidragService
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
@StepScope
class GjennomførIndeksreguleringBidragBatchProcessor(
    private val indeksreguleringBidragService: IndeksreguleringBidragService,
    private val indeksreguleringRepository: IndeksreguleringRepository,
) : ItemProcessor<Indeksregulering, Indeksregulering> {
    private var simuler: Boolean = false

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
    }

    override fun process(indeksregulering: Indeksregulering): Indeksregulering? =
        try {
            if (indeksregulering.status == Status.BEHANDLET) {
                LOGGER.info { "Indeksregulering ${indeksregulering.id} er allerede behandlet. Hopper over." }
                return null
            }
            // TODO(Gjennomføring av indeksregulering med vedtaksforslag)
            indeksreguleringBidragService.gjennomførIndeksregulering(indeksregulering, simuler)
        } catch (e: Exception) {
            LOGGER.error(e) {
                "Det skjedde en feil ved gjennomføring av indeksregulering bidrag for sak ${indeksregulering.barn.saksnummer}. Hopper over saken."
            }
            indeksregulering.also {
                it.status = Status.FEILET
                indeksreguleringRepository.save(it)
            }
            null
        }
}
