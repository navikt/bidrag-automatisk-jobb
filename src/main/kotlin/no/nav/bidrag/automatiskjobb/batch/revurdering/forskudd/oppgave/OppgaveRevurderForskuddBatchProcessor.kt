package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.oppgave

import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class OppgaveRevurderForskuddBatchProcessor : ItemProcessor<Any, Unit> {
    override fun process(item: Any) {
        TODO("Not yet implemented")
    }
}
