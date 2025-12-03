package no.nav.bidrag.automatiskjobb.batch.forsendelse.opprett

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OpprettForsendelseBatch(
    private val jobLauncher: JobLauncher,
    private val opprettForsendelseJob: Job,
) {
    fun start(prosesserFeilet: Boolean = false) {
        jobLauncher.run(
            opprettForsendelseJob,
            JobParametersBuilder()
                .addString("prosesserFeilet", prosesserFeilet.toString())
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
