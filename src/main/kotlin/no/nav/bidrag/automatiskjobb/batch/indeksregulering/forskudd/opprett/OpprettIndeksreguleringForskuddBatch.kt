package no.nav.bidrag.automatiskjobb.batch.indeksregulering.forskudd.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Year
import java.util.UUID

private val LOGGER = KotlinLogging.logger { }

@Component
class OpprettIndeksreguleringForskuddBatch(
    @param:Qualifier("asyncJobLauncher") private val jobOperator: JobOperator,
    private val opprettIndeksreguleringForskuddJob: Job,
) {
    fun start(
        saksnummer: List<String> = emptyList(),
        år: Int = Year.now().value,
    ) {
        try {
            jobOperator.start(
                opprettIndeksreguleringForskuddJob,
                JobParametersBuilder()
                    .addString("batchId", UUID.randomUUID().toString())
                    .addString("aar", år.toString())
                    .apply {
                        if (saksnummer.isNotEmpty()) {
                            addString("saksnummer", saksnummer.joinToString(","))
                        }
                    }.toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch opprettIndeksreguleringForskudd kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
