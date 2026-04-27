package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

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
class OpprettRevurderForskuddBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val opprettRevurderForskuddJob: Job,
) {
    fun start(månederTilbakeForManueltVedtak: Int = 12) {
        try {
            jobOperator.start(
                opprettRevurderForskuddJob,
                JobParametersBuilder()
                    .addString("batchId", UUID.randomUUID().toString())
                    .addString("månederTilbakeForManueltVedtak", månederTilbakeForManueltVedtak.toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch opprettRevurderForskudd kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
