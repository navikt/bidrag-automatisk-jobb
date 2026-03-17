package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

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
class BeregnAldersjusteringerBidragBatch(
    @param:Qualifier("asyncJobLauncher") private val jobLauncher: JobLauncher,
    private val beregnAldersjusteringerBidragJob: Job,
) {
    fun startBeregnAldersjusteringBidragBatch(
        simuler: Boolean,
        barn: String?,
    ) {
        try {
            jobLauncher.run(
                beregnAldersjusteringerBidragJob,
                JobParametersBuilder()
                    .addString("barn", barn ?: "")
                    .addString("simuler", simuler.toString())
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch beregnAldersjusteringerBidrag kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
