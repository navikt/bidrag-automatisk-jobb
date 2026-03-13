package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak

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
class FatteVedtakRevurderForskuddBatch(
    @param:Qualifier("asyncJobLauncher") private val jobLauncher: JobLauncher,
    private val fatteVedtakRevurderForskuddJob: Job,
) {
    fun start(simuler: Boolean) {
        try {
            jobLauncher.run(
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
