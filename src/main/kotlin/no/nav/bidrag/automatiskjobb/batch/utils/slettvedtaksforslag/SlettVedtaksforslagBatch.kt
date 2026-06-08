package no.nav.bidrag.automatiskjobb.batch.utils.slettvedtaksforslag

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
class SlettVedtaksforslagBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val slettVedtaksforslagJob: Job,
) {
    fun startSlettVedtaksforslagBatch(
        inkluderBehandlet: Boolean = false,
        barn: String? = null,
    ) {
        try {
            jobOperator.start(
                slettVedtaksforslagJob,
                JobParametersBuilder()
                    .addString("inkluderBehandlet", inkluderBehandlet.toString())
                    .addString("barn", barn ?: "")
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch slettVedtaksforslag kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
