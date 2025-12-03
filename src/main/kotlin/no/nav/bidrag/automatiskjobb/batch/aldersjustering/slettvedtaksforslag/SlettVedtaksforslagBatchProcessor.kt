package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@StepScope
class SlettVedtaksforslagBatchProcessor(
    private val aldersjusteringService: AldersjusteringService,
) : ItemProcessor<Aldersjustering, Aldersjustering> {
    override fun process(item: Aldersjustering) =
        try {
            aldersjusteringService.slettVedtaksforslag(
                aldersjustering = item,
            )
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved prosessering av aldersjustering ${item.id}" }
            null
        }
}
