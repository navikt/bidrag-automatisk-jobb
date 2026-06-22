package no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.distribuer

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
class DistribuerForsendelseBidragBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val distribuerForsendelseJob: Job,
) {
    fun start(bestillingIder: String? = null) {
        try {
            val paramsBuilder =
                JobParametersBuilder()
                    .addString("runId", UUID.randomUUID().toString())
            if (!bestillingIder.isNullOrEmpty()) {
                paramsBuilder.addString("bestillingIds", bestillingIder)
            }
            jobOperator.start(
                distribuerForsendelseJob,
                paramsBuilder.toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch distribuerForsendelse kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
