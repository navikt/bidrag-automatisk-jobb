package no.nav.bidrag.automatiskjobb.batch.forsendelse.distribuer

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DistribuerForsendelseAldersjusteringerBidragBatch(
    private val jobLauncher: JobLauncher,
    private val distribuerForsendelseAldersjusteringerBidragJob: Job,
) {
    fun startDistribuerForsendelseAldersjusteringBidragBatch() {
        jobLauncher.run(
            distribuerForsendelseAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
