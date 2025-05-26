package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BeregnAldersjusteringerBidragBatch(
    private val jobLauncher: JobLauncher,
    private val beregnAldersjusteringerBidragJob: Job,
) {
    fun startBeregnAldersjusteringBidragBatch(
        simuler: Boolean,
        barn: String?,
    ) {
        jobLauncher.run(
            beregnAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addString("barn", barn ?: "")
                .addString("simuler", simuler.toString())
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
