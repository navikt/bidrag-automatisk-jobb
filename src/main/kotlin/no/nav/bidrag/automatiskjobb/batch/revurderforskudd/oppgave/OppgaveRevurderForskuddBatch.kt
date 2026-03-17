package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.oppgave

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

private val LOGGER = KotlinLogging.logger { }

@Deprecated("Bruk revurderingslenke i stedet")
@Component
class OppgaveRevurderForskuddBatch(
    @param:Qualifier("asyncJobLauncher") private val jobLauncher: JobLauncher,
    private val oppgaveRevurderForskuddJob: Job,
) {
    fun start() {
        try {
            jobLauncher.run(
                oppgaveRevurderForskuddJob,
                JobParametersBuilder()
                    .addString("batchId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch oppgaveRevurderForskudd kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
