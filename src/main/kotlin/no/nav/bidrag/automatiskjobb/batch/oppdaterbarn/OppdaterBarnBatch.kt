package no.nav.bidrag.automatiskjobb.batch.oppdaterbarn

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
class OppdaterBarnBatch(
    @param:Qualifier("asyncJobLauncher") private val jobLauncher: JobLauncher,
    private val oppdaterBarnJob: Job,
) {
    fun startOppdaterBarnBatch(
        barnId: String? = "",
        simuler: Boolean,
    ) {
        try {
            jobLauncher.run(
                oppdaterBarnJob,
                JobParametersBuilder()
                    .addString("simuler", simuler.toString())
                    .addString("barn", barnId ?: "")
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch oppdaterBarn kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
