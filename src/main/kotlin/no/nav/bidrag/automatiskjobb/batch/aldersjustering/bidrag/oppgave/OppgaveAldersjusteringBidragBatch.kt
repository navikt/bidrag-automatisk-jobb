package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OppgaveAldersjusteringBidragBatch(
    private val jobLauncher: JobLauncher,
    private val oppgaveAldersjusteringerBidragJob: Job,
) {
    fun startOppgaveAldersjusteringBidragBatch(barnId: String = "") {
        jobLauncher.run(
            oppgaveAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addString("barn", barnId)
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
