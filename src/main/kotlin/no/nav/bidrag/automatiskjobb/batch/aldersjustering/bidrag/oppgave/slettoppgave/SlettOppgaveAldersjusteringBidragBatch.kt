package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.slettoppgave

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SlettOppgaveAldersjusteringBidragBatch(
    private val jobLauncher: JobLauncher,
    private val slettOppgaveAldersjusteringerBidragJob: Job,
) {
    fun startSlettOppgaveAldersjusteringBidragBatch(
        barnId: String? = "",
        batchId: String,
    ) {
        jobLauncher.run(
            slettOppgaveAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addString("barn", barnId ?: "")
                .addString("batchId", batchId)
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
