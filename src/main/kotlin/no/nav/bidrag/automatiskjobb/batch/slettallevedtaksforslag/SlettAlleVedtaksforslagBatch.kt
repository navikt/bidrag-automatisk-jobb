package no.nav.bidrag.automatiskjobb.batch.slettallevedtaksforslag

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
class SlettAlleVedtaksforslagBatch(
    @param:Qualifier("asyncJobLauncher") private val jobLauncher: JobLauncher,
    private val slettAlleVedtaksforslagJob: Job,
) {
    fun startAlleSlettVedtaksforslagBatch() {
        try {
            jobLauncher.run(
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
