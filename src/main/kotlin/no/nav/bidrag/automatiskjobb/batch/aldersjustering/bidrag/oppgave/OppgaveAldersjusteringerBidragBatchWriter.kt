package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
@StepScope
class OppgaveAldersjusteringerBidragBatchWriter(
    private val aldersjusteringService: AldersjusteringService,
) : ItemWriter<Aldersjustering> {
    override fun write(chunk: Chunk<out Aldersjustering>) {
        chunk.forEach { aldersjustering ->
            aldersjusteringService.opprettOppgaveForAldersjustering(aldersjustering)
        }
    }
}
