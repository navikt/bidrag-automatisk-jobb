package no.nav.bidrag.automatiskjobb.batch.utils.slettallevedtaksforslag

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
class SlettAlleVedtaksforslagBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val slettAlleVedtaksforslagJob: Job,
) {
    fun startAlleSlettVedtaksforslagBatch() {
        try {
            jobOperator.start(
                slettAlleVedtaksforslagJob,
                JobParametersBuilder()
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch slettAlleVedtaksforslag kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
