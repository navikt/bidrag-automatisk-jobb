package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.fattvedtak

import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class FatteVedtakRevurderForskuddBatchProcessor : ItemProcessor<Any, Unit> {
    override fun process(item: Any) {
        TODO("Not yet implemented")
    }
}
