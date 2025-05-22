package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.slettoppgave

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@StepScope
class SlettOppgaveAldersjusteringerBidragBatchProcessor(
    private val aldersjusteringService: AldersjusteringService,
) : ItemProcessor<Aldersjustering, Int?> {
    override fun process(aldersjustering: Aldersjustering): Int? =
        try {
            aldersjusteringService.slettOppgaveForAldersjustering(aldersjustering)
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved sletting av oppgave for aldersjustering ${aldersjustering.id}" }
            null
        }
}
