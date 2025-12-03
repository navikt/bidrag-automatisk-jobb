package no.nav.bidrag.automatiskjobb.batch.forsendelse.distribuer

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DistribuerForsendelseBidragBatch(
    private val jobLauncher: JobLauncher,
    private val distribuerForsendelseJob: Job,
) {
    fun start() {
        jobLauncher.run(
            distribuerForsendelseJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
