package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FattVedtakOmAldersjusteringerBidragBatch(
    private val jobLauncher: JobLauncher,
    private val fattVedtakOmAldersjusteringerBidragJob: Job,
) {
    fun startFattVedtakOmAldersjusteringBidragBatch(barnId: String = "") {
        jobLauncher.run(
            fattVedtakOmAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addString("barn", barnId)
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
