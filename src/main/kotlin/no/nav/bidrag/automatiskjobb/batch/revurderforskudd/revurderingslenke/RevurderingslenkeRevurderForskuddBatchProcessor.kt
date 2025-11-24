package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class RevurderingslenkeRevurderForskuddBatchProcessor(
    private val revurderForskuddService: RevurderForskuddService,
) : ItemProcessor<RevurderingForskudd, Int?> {
    override fun process(revurderingForskudd: RevurderingForskudd): Int? =
        try {
            revurderForskuddService.opprettRevurderingslenke(revurderingForskudd)
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved opprettelse av oppgave for aldersjustering ${revurderingForskudd.id}" }
            null
        }
}
