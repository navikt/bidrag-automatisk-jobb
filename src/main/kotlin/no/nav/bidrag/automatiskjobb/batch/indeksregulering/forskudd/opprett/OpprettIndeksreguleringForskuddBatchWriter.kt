package no.nav.bidrag.automatiskjobb.batch.indeksregulering.forskudd.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class OpprettIndeksreguleringForskuddBatchWriter(
    private val indeksreguleringRepository: IndeksreguleringRepository,
) : ItemWriter<Indeksregulering> {
    override fun write(chunk: Chunk<out Indeksregulering>) {
        indeksreguleringRepository.saveAll(
            chunk.map {
                LOGGER.info {
                    "Lagrer indeksregulering forskudd for sak ${it.saksnummer} med barn id=${
                        it.barn.map { barn -> barn.id }.joinToString()
                    } med status ${it.status}"
                }
                it
            },
        )
    }
}
