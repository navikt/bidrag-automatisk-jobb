package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.oppgave

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OppgaveRevurderForskuddBatch(
    private val jobLauncher: JobLauncher,
    private val oppgaveRevurderForskuddJob: Job,
) {
    fun startOppgaveRevurderForskuddBatch(
        simuler: Boolean,
        barn: String?,
    ) {
        jobLauncher.run(
            oppgaveRevurderForskuddJob,
            JobParametersBuilder()
                .addString("simuler", simuler.toString())
                .addString("barn", barn ?: "")
                .addString("batchId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
