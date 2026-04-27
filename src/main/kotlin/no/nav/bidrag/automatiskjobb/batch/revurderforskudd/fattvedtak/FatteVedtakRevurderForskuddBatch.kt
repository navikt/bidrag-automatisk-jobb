package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak

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
class FatteVedtakRevurderForskuddBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val fatteVedtakRevurderForskuddJob: Job,
) {
    fun start(simuler: Boolean) {
        try {
            jobOperator.start(
                fatteVedtakRevurderForskuddJob,
                JobParametersBuilder()
                    .addString("simuler", simuler.toString())
                    .addString("batchId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch fatteVedtakRevurderForskudd kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
