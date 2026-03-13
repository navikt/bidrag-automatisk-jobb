package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

private val LOGGER = KotlinLogging.logger { }

@Component
class RevurderingslenkeRevurderForskuddBatch(
    @param:Qualifier("asyncJobLauncher") private val jobLauncher: JobLauncher,
    private val revurderingslenkeRevurderForskuddJob: Job,
) {
    fun start(søktFraDato: LocalDate?) {
        try {
            jobLauncher.run(
                revurderingslenkeRevurderForskuddJob,
                JobParametersBuilder()
                    .addString("batchId", UUID.randomUUID().toString())
                    .apply {
                        søktFraDato?.let { addString("soktFraDato", søktFraDato.toString()) }
                    }.toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch revurderingslenkeRevurderForskudd kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
