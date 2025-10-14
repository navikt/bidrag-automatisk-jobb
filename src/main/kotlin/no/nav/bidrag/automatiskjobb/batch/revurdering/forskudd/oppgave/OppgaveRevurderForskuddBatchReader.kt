package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.oppgave

import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component

@Component
class OppgaveRevurderForskuddBatchReader : ItemReader<Any> {
    override fun read(): Any {
        TODO("Not yet implemented")
    }
}
