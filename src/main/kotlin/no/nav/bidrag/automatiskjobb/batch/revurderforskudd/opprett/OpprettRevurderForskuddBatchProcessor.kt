package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd.OpprettRevurderForskuddService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger { }

@Component
class OpprettRevurderForskuddBatchProcessor(
    private val opprettRevurderForskuddService: OpprettRevurderForskuddService,
) : ItemProcessor<List<Barn>, RevurderingForskudd> {
    private lateinit var batchId: String
    private lateinit var månederTilbakeForManueltVedtak: LocalDateTime

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        batchId = stepExecution.jobParameters.getString("batchId")!!
        månederTilbakeForManueltVedtak =
            stepExecution.jobParameters
                .getString("månederTilbakeForManueltVedtak")
                ?.toLong()
                .let { LocalDateTime.now().minusMonths(it!!) }
    }

    override fun process(barn: List<Barn>): RevurderingForskudd? =
        try {
            opprettRevurderForskuddService.opprettRevurdereForskudd(barn, batchId, månederTilbakeForManueltVedtak)
        } catch (e: Exception) {
            LOGGER.error(e) {
                "Det skjedde en feil ved oppretting av revurdering forskudd for sak ${barn.first().saksnummer}. Hopper over saken."
            }
            null
        }
}
