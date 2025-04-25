package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
@StepScope
class BeregnAldersjusteringerBidragBatchWriter(
    private val aldersjusteringService: AldersjusteringService,
) : ItemWriter<Aldersjustering> {
    private var simuler: Boolean = true

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
    }

    override fun write(chunk: Chunk<out Aldersjustering>) {
        chunk.forEach { aldersjustering ->
            aldersjusteringService.utførAldersjustering(
                aldersjustering = aldersjustering,
                stønadstype = Stønadstype.BIDRAG,
                simuler,
            )
        }
    }
}
