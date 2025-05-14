package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
@StepScope
class OpprettAldersjusteringerBidragBatchProcessor(
    private val aldersjusteringService: AldersjusteringService,
) : ItemProcessor<Barn, Aldersjustering?> {
    private var år: Long? = -1
    private var runId: String? = ""

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        runId = stepExecution.jobParameters.getString("runId")
        år = stepExecution.jobParameters.getLong("år")
    }

    override fun process(barn: Barn): Aldersjustering? {
        val aldersjustering =
            aldersjusteringService.opprettAldersjusteringForBarn(barn, år!!.toInt(), runId!!, Stønadstype.BIDRAG)
        return aldersjustering
    }
}
