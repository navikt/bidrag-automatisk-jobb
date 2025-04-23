package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprettaldersjustering

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
@StepScope
class AldersjusteringBidragOpprettAldersjusteringerBatchWriter(
//    @Value("#{jobParameters['år']}") år: Long? = -1,
//    @Value("#{jobParameters['runId']}") runId: String? = "",
    private val aldersjusteringService: AldersjusteringService,
) : ItemWriter<Barn> {
    private var år: Long? = -1
    private var runId: String? = ""

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        runId = stepExecution.jobParameters.getString("runId")
        år = stepExecution.jobParameters.getLong("år")
    }

    override fun write(chunk: Chunk<out Barn>) {
        chunk.forEach { barn ->
            aldersjusteringService.opprettAldersjusteringForBarn(
                barn = barn,
                år = år!!.toInt(),
                batchId = runId!!,
            )
        }
    }
}
