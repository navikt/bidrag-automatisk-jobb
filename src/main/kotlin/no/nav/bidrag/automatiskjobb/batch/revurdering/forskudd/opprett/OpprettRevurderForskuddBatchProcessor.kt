package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger { }

@Component
class OpprettRevurderForskuddBatchProcessor(
    private val revurderForskuddService: RevurderForskuddService,
) : ItemProcessor<Barn, RevurderingForskudd> {
    private lateinit var batchId: String
    private lateinit var cutoffTidspunktForManueltVedtak: LocalDateTime

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        batchId = stepExecution.jobParameters.getString("batchId")!!
        cutoffTidspunktForManueltVedtak =
            stepExecution.jobParameters
                .getString("antallManederTilbake")
                ?.toLong()
                .let { LocalDateTime.now().minusMonths(it!!) }
    }

    override fun process(barn: Barn): RevurderingForskudd? =
        revurderForskuddService.opprettRevurdereForskudd(barn, batchId, cutoffTidspunktForManueltVedtak)
}
