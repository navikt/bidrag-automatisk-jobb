package no.nav.bidrag.automatiskjobb.batch.bidrag.slettvedtaksforslag

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component

@Component
class AldersjusteringBidragSlettVedtaksforslagBatch(
    private val jobLauncher: JobLauncher,
    private val aldersjusteringBidragSlettVedtaksforslagJob: Job,
) {
    fun startAldersjusteringSlettVedtaksforslagBatch() {
        jobLauncher.run(
            aldersjusteringBidragSlettVedtaksforslagJob,
            JobParametersBuilder()
                .toJobParameters(),
        )
    }
}
