package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.opprett

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OpprettRevurderForskuddBatch(
    private val jobLauncher: JobLauncher,
    private val opprettRevurderForskuddJob: Job,
) {
    fun startRevurderForskuddBatch(
        simuler: Boolean,
        barn: String?,
    ) {
        jobLauncher.run(
            opprettRevurderForskuddJob,
            JobParametersBuilder()
                .addString("simuler", simuler.toString())
                .addString("barn", barn ?: "")
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
