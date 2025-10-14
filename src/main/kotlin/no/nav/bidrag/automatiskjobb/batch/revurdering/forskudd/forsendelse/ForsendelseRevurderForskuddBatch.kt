package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.forsendelse

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ForsendelseRevurderForskuddBatch(
    private val jobLauncher: JobLauncher,
    private val forsendelseRevurderForskuddJob: Job,
) {
    fun startForsendelseRevurderForskuddBatch(
        simuler: Boolean,
        barn: String?,
    ) {
        jobLauncher.run(
            forsendelseRevurderForskuddJob,
            JobParametersBuilder()
                .addString("simuler", simuler.toString())
                .addString("barn", barn ?: "")
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
