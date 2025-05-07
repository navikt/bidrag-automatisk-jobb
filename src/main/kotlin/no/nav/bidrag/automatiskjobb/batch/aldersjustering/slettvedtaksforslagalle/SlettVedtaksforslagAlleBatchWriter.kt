package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslagalle

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class SlettVedtaksforslagAlleBatchWriter(
    private val vedtakConsumer: BidragVedtakConsumer,
) : ItemWriter<List<Int>?> {
    override fun write(chunk: Chunk<out List<Int>?>) {
        chunk.forEach { vedtaksider ->
            vedtaksider?.forEach { vedtaksid ->
                log.info { "Sletter vedtaksforslag $vedtaksid" }
                vedtakConsumer.slettVedtaksforslag(
                    vedtaksid,
                )
            }
        }
    }
}
