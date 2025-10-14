package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.opprett

import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class OpprettRevurderForskuddBatchProcessor : ItemProcessor<Any, Unit> {
    override fun process(item: Any) {
        TODO("Not yet implemented")
    }
}
