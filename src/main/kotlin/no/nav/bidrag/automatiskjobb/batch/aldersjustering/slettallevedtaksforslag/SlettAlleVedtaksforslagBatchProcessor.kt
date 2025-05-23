package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettallevedtaksforslag

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class SlettAlleVedtaksforslagBatchProcessor(
    private val vedtakConsumer: BidragVedtakConsumer,
) : ItemProcessor<List<Int>, List<Int>> {
    override fun process(vedtaksider: List<Int>) =
        vedtaksider.mapNotNull { vedtaksid ->
            try {
                log.info { "Sletter vedtaksforslag $vedtaksid" }
                vedtakConsumer.slettVedtaksforslag(
                    vedtaksid,
                )
                vedtaksid
            } catch (e: Exception) {
                log.error(e) {
                    "Det skjedde en feil ved prosessering av aldersjustering $vedtaksid"
                }
                null
            }
        }
}
