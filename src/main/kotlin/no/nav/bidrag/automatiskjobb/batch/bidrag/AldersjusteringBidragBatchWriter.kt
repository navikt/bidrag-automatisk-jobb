package no.nav.bidrag.automatiskjobb.batch.bidrag

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class AldersjusteringBidragBatchWriter(
    private val aldersjusteringService: AldersjusteringService,
) : ItemWriter<Barn?> {
    private var jobExecutionId: String = ""
    private var år: Long? = -1
    private var simuler: Boolean = true

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        jobExecutionId = stepExecution.jobExecutionId.toString()
        år = stepExecution.jobParameters.getLong("år")
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
    }

    override fun write(chunk: Chunk<out Barn>) {
        chunk.forEach { barn ->
            aldersjusteringService.utførAldersjusteringForBarn(
                stønadstype = Stønadstype.BIDRAG,
                barn = barn,
                år = år!!.toInt(),
                batchId = jobExecutionId,
                simuler = simuler,
            )
        }
    }
}
