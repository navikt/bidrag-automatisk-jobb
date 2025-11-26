package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RevurderingslenkeRevurderForskuddBatch(
    private val jobLauncher: JobLauncher,
    private val revurderingslenkeRevurderForskuddJob: Job,
) {
    fun start() {
        jobLauncher.run(
            revurderingslenkeRevurderForskuddJob,
            JobParametersBuilder()
                .addString("batchId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
