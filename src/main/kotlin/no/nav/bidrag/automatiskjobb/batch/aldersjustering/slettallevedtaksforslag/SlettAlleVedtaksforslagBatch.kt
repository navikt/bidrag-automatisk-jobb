package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettallevedtaksforslag

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SlettAlleVedtaksforslagBatch(
    private val jobLauncher: JobLauncher,
    private val slettVedtaksforslagAlleJob: Job,
) {
    fun startAlleSlettVedtaksforslagBatch() {
        jobLauncher.run(
            slettVedtaksforslagAlleJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
