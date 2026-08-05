package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class OpprettIndeksreguleringBidragBatchWriter(
    private val indeksreguleringRepository: IndeksreguleringRepository,
) : ItemWriter<List<Indeksregulering>> {
    override fun write(chunk: Chunk<out List<Indeksregulering>>) {
        indeksreguleringRepository.saveAll(
            chunk.flatten(),
        )
    }
}
