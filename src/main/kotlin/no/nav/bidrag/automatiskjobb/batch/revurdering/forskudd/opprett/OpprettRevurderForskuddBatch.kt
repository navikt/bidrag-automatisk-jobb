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
    fun start(antallMånederTilbake: Int = 12) {
        jobLauncher.run(
            opprettRevurderForskuddJob,
            JobParametersBuilder()
                .addString("batchId", UUID.randomUUID().toString())
                .addString("antallManederTilbake", antallMånederTilbake.toString())
                .toJobParameters(),
        )
    }
}
