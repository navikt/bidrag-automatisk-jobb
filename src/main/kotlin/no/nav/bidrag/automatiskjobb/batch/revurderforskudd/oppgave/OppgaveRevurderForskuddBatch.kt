package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.oppgave

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Deprecated("Bruk revurderingslenke i stedet")
@Component
class OppgaveRevurderForskuddBatch(
    private val jobLauncher: JobLauncher,
    private val oppgaveRevurderForskuddJob: Job,
) {
    fun start() {
        jobLauncher.run(
            oppgaveRevurderForskuddJob,
            JobParametersBuilder()
                .addString("batchId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
