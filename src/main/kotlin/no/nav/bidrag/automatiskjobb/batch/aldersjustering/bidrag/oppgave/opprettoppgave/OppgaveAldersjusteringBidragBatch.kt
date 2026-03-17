package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.opprettoppgave

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

private val LOGGER = KotlinLogging.logger { }

@Component
class OppgaveAldersjusteringBidragBatch(
    @param:Qualifier("asyncJobLauncher") private val jobLauncher: JobLauncher,
    private val oppgaveAldersjusteringerBidragJob: Job,
) {
    fun startOppgaveAldersjusteringBidragBatch(barnId: String?) {
        try {
            jobLauncher.run(
                oppgaveAldersjusteringerBidragJob,
                JobParametersBuilder()
                    .addString("barn", barnId ?: "")
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch oppgaveAldersjusteringBidrag kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
