package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.gjennomfor

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class GjennomførIndeksreguleringBidragBatchWriter(
    private val indeksreguleringRepository: IndeksreguleringRepository,
) : ItemWriter<Indeksregulering> {
    override fun write(chunk: Chunk<out Indeksregulering>) {
        indeksreguleringRepository.saveAll(
            chunk.map {
                LOGGER.info {
                    "Lagrer gjennomført indeksregulering bidrag for sak ${it.saksnummer} med status ${it.status}"
                }
                it
            },
        )
    }
}
