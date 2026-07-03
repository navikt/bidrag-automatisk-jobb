package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.gjennomfor

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.IndeksreguleringBidragService
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class GjennomførIndeksreguleringBidragBatchProcessor(
    private val indeksreguleringBidragService: IndeksreguleringBidragService,
    private val indeksreguleringRepository: IndeksreguleringRepository,
) : ItemProcessor<Indeksregulering, Indeksregulering> {
    override fun process(indeksregulering: Indeksregulering): Indeksregulering? =
        try {
            if (indeksregulering.status == Status.BEHANDLET) {
                LOGGER.info { "Indeksregulering ${indeksregulering.id} er allerede behandlet. Hopper over." }
                return null
            }
            indeksreguleringBidragService.gjennomførIndeksregulering(indeksregulering)
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
