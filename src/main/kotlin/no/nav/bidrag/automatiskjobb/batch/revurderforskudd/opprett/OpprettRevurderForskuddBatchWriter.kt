package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class OpprettRevurderForskuddBatchWriter(
    private val revurderForskuddRepository: RevurderForskuddRepository,
) : ItemWriter<RevurderingForskudd> {
    override fun write(chunk: Chunk<out RevurderingForskudd?>) {
        revurderForskuddRepository.saveAll(
            chunk.filterNotNull().map {
                LOGGER.info { "Lagrer opprettelse av revurdering forskudd for barn med id=${it.barn.id} med status ${it.status}" }
                it
            },
        )
    }
}
