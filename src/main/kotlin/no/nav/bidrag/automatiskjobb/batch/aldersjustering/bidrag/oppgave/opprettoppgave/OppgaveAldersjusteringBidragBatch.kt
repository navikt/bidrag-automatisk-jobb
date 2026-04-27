package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.opprettoppgave

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

private val LOGGER = KotlinLogging.logger { }

@Component
class OppgaveAldersjusteringBidragBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val oppgaveAldersjusteringerBidragJob: Job,
) {
    fun startOppgaveAldersjusteringBidragBatch(barnId: String?) {
        try {
            jobOperator.start(
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
