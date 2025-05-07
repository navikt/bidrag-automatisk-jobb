package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslagalle

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SlettVedtaksforslagAlleBatch(
    private val jobLauncher: JobLauncher,
    private val slettVedtaksforslagAlleJob: Job,
) {
    fun startSlettVedtaksforslagBatch() {
        jobLauncher.run(
            slettVedtaksforslagAlleJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
