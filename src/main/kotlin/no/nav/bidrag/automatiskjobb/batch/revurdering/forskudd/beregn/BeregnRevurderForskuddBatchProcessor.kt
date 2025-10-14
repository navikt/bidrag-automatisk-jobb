package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.beregn

import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class BeregnRevurderForskuddBatchProcessor : ItemProcessor<Any, Unit> {
    override fun process(item: Any) {
        TODO("Not yet implemented")
    }
}
