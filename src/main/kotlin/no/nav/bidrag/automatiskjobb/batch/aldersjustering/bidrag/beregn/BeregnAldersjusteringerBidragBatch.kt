package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

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
class BeregnAldersjusteringerBidragBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val beregnAldersjusteringerBidragJob: Job,
) {
    fun startBeregnAldersjusteringBidragBatch(
        simuler: Boolean,
        inkluderBehandlet: Boolean,
        barn: String?,
    ) {
        try {
            jobOperator.start(
                beregnAldersjusteringerBidragJob,
                JobParametersBuilder()
                    .addString("barn", barn ?: "")
                    .addString("inkluderBehandlet", inkluderBehandlet.toString())
                    .addString("simuler", simuler.toString())
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch beregnAldersjusteringerBidrag kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
