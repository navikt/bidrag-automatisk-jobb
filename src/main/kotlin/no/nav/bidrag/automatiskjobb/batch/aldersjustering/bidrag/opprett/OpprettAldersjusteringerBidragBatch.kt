package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

private val LOGGER = KotlinLogging.logger { }

@Component
class OpprettAldersjusteringerBidragBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val opprettAldersjusteringerBidragJob: Job,
) {
    fun startOpprettAldersjusteringBidragBatch(
        aldersjusteringsdato: LocalDate?,
        år: Long,
    ) {
        try {
            jobOperator.start(
                opprettAldersjusteringerBidragJob,
                JobParametersBuilder()
                    .addLocalDate(
                        "aldersjusteringsdato",
                        aldersjusteringsdato ?: LocalDate
                            .now()
                            .withYear(år.toInt())
                            .withMonth(7)
                            .withDayOfMonth(1),
                    ).addLong("år", år)
                    .addString("batchId", "aldersjustering_bidrag_$år")
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch opprettAldersjusteringerBidrag kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
