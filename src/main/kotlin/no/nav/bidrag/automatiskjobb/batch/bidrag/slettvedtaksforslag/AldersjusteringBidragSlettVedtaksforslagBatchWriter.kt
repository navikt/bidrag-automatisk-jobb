package no.nav.bidrag.automatiskjobb.batch.bidrag.slettvedtaksforslag

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class AldersjusteringBidragSlettVedtaksforslagBatchWriter(
    private val aldersjusteringService: AldersjusteringService,
) : ItemWriter<Aldersjustering?> {
    override fun write(chunk: Chunk<out Aldersjustering>) {
        chunk.forEach { aldersjustering ->
            aldersjusteringService.slettVedtaksforslag(
                stønadstype = Stønadstype.BIDRAG,
                aldersjustering = aldersjustering,
            )
        }
    }
}
