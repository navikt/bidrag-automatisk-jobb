package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.oppgave

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd.OppgaveRevurderingForskuddService
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
@Deprecated("Bruk revurderingslenke i stedet")
class OppgaveRevurderForskuddBatchProcessor(
    private val oppgaveRevurderingForskuddService: OppgaveRevurderingForskuddService,
) : ItemProcessor<RevurderingForskudd, Int> {
    override fun process(revurderingForskudd: RevurderingForskudd): Int? =
        try {
            oppgaveRevurderingForskuddService.opprettOppgave(revurderingForskudd)
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved opprettelse av oppgave for aldersjustering ${revurderingForskudd.id}" }
            null
        }
}
