package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

private val LOGGER = KotlinLogging.logger { }

@Component
class RevurderingslenkeRevurderForskuddBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val revurderingslenkeRevurderForskuddJob: Job,
) {
    fun start(
        søktFraDato: LocalDate?,
        forMåned: YearMonth?,
    ) {
        try {
            jobOperator.start(
                revurderingslenkeRevurderForskuddJob,
                JobParametersBuilder()
                    .addString("batchId", UUID.randomUUID().toString())
                    .apply {
                        søktFraDato?.let { addString("soktFraDato", søktFraDato.toString()) }
                        forMåned?.let { addString("forManed", forMåned.toString()) }
                    }.toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch revurderingslenkeRevurderForskudd kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
