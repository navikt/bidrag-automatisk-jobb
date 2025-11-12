package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.oppgave

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component


private val LOGGER = KotlinLogging.logger { }

@Component
class OppgaveRevurderForskuddBatchProcessor(
    private val revurderingForskuddService: RevurderForskuddService
) : ItemProcessor<RevurderingForskudd, Int> {
    override fun process(revurderingForskudd: RevurderingForskudd): Int? {
        return try {
            revurderingForskuddService.opprettOppgave(revurderingForskudd)
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved opprettelse av oppgave for aldersjustering ${revurderingForskudd.id}" }
            null
        }
    }
}
