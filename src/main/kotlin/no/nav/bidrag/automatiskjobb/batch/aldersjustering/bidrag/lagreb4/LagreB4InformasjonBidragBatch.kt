package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.lagreb4

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

private val LOGGER = KotlinLogging.logger {}

@Component
class LagreB4InformasjonBidragBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val lagreB4InformasjonBidragJob: Job,
) {
    fun startLagreB4InformasjonBidragBatch(
        fattetÅr: Long,
        barn: String?,
    ) {
        try {
            jobOperator.start(
                lagreB4InformasjonBidragJob,
                JobParametersBuilder()
                    .addLong("fattetÅr", fattetÅr)
                    .addString("barn", barn ?: "")
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch lagreB4InformasjonBidrag kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
