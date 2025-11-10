package no.nav.bidrag.automatiskjobb.batch.slettallevedtaksforslag

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SlettAlleVedtaksforslagBatch(
    private val jobLauncher: JobLauncher,
    private val slettAlleVedtaksforslagJob: Job,
) {
    fun startAlleSlettVedtaksforslagBatch() {
        jobLauncher.run(
            slettAlleVedtaksforslagJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
