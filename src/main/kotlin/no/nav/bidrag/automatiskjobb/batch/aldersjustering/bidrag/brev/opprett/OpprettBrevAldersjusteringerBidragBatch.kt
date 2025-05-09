package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.opprett

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OpprettBrevAldersjusteringerBidragBatch(
    private val jobLauncher: JobLauncher,
    private val opprettBrevAldersjusteringerBidragJob: Job,
) {
    fun startOpprettBrevAldersjusteringBidragBatch() {
        jobLauncher.run(
            opprettBrevAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
