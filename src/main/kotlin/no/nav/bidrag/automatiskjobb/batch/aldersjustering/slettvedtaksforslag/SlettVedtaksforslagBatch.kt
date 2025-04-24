package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SlettVedtaksforslagBatch(
    private val jobLauncher: JobLauncher,
    private val aldersjusteringBidragSlettVedtaksforslagJob: Job,
) {
    fun startSlettVedtaksforslagBatch() {
        jobLauncher.run(
            aldersjusteringBidragSlettVedtaksforslagJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
