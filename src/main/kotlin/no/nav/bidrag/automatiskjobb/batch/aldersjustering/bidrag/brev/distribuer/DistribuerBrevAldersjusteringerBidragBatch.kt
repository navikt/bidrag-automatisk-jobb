package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.distribuer

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DistribuerBrevAldersjusteringerBidragBatch(
    private val jobLauncher: JobLauncher,
    private val distribuerBrevAldersjusteringerBidragJob: Job,
) {
    fun startDistribuerBrevAldersjusteringBidragBatch() {
        jobLauncher.run(
            distribuerBrevAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
