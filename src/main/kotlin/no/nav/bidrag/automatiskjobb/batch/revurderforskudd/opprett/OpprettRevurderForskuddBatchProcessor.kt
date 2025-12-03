package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd.OpprettRevurderForskuddService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class OpprettRevurderForskuddBatchProcessor(
    private val opprettRevurderForskuddService: OpprettRevurderForskuddService,
) : ItemProcessor<Barn, RevurderingForskudd> {
    private lateinit var batchId: String
    private lateinit var månederTilbakeForManueltVedtak: LocalDateTime

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        batchId = stepExecution.jobParameters.getString("batchId")!!
        månederTilbakeForManueltVedtak =
            stepExecution.jobParameters
                .getString("manederTilbakeForManueltVedtak")
                ?.toLong()
                .let { LocalDateTime.now().minusMonths(it!!) }
    }

    override fun process(barn: Barn): RevurderingForskudd? =
        opprettRevurderForskuddService.opprettRevurdereForskudd(barn, batchId, månederTilbakeForManueltVedtak)
}
