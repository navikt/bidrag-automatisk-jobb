package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.fattvedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Year
import java.util.UUID

private val LOGGER = KotlinLogging.logger { }

@Component
class FattVedtakIndeksreguleringBidragBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val fattVedtakIndeksreguleringBidragJob: Job,
) {
    fun start(
        år: Int = Year.now().value,
        simuler: Boolean = true,
    ) {
        try {
            jobOperator.start(
                fattVedtakIndeksreguleringBidragJob,
                JobParametersBuilder()
                    .addString("batchId", UUID.randomUUID().toString())
                    .addString("aar", år.toString())
                    .addString("simuler", simuler.toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch fattVedtakIndeksreguleringBidrag kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
