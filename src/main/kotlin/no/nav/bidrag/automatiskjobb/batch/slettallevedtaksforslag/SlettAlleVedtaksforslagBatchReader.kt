package no.nav.bidrag.automatiskjobb.batch.slettallevedtaksforslag

import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component

@Component
@StepScope
class SlettAlleVedtaksforslagBatchReader(
    val vedtakConsumer: BidragVedtakConsumer,
) : ItemReader<List<Int>?> {
    override fun read(): List<Int>? = vedtakConsumer.hentAlleVedtaksforslag(500).takeIf { it.isNotEmpty() }
}
