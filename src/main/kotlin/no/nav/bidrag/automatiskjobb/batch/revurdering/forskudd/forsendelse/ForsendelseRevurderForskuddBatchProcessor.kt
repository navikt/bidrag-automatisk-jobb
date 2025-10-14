package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.forsendelse

import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class ForsendelseRevurderForskuddBatchProcessor : ItemProcessor<Any, Unit> {
    override fun process(item: Any) {
        TODO("Not yet implemented")
    }
}
