package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

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
class FattVedtakOmAldersjusteringerBidragBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val fattVedtakOmAldersjusteringerBidragJob: Job,
) {
    fun startFattVedtakOmAldersjusteringBidragBatch(
        barnId: String? = "",
        simuler: Boolean,
        behandlingstyper: String = "MANUELL,FATTET_FORSLAG,INGEN",
        kunRedusertBidrag: Boolean = false,
    ) {
        try {
            jobOperator.start(
                fattVedtakOmAldersjusteringerBidragJob,
                JobParametersBuilder()
                    .addString("simuler", simuler.toString())
                    .addString("barn", barnId ?: "")
                    .addString("behandlingstyper", behandlingstyper)
                    .addString("kunRedusertBidrag", kunRedusertBidrag.toString())
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch fattVedtakOmAldersjusteringerBidrag kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
