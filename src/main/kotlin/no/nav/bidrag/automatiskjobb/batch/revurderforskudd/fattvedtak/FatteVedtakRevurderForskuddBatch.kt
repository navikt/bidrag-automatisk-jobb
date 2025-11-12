package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FatteVedtakRevurderForskuddBatch(
    private val jobLauncher: JobLauncher,
    private val fatteVedtakRevurderForskuddJob: Job,
) {
    fun start(simuler: Boolean) {
        jobLauncher.run(
            fatteVedtakRevurderForskuddJob,
            JobParametersBuilder()
                .addString("simuler", simuler.toString())
                .addString("batchId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
