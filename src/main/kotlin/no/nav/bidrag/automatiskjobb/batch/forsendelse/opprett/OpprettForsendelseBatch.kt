package no.nav.bidrag.automatiskjobb.batch.forsendelse.opprett

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
class OpprettForsendelseBatch(
    @param:Qualifier("asyncJobLauncher") private val jobLauncher: JobLauncher,
    private val opprettForsendelseJob: Job,
) {
    fun start(prosesserFeilet: Boolean = false) {
        try {
            jobLauncher.run(
                opprettForsendelseJob,
                JobParametersBuilder()
                    .addString("prosesserFeilet", prosesserFeilet.toString())
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch opprettForsendelse kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
