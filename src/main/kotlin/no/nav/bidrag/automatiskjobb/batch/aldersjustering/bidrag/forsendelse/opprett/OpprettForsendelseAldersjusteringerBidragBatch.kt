package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.opprett

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OpprettForsendelseAldersjusteringerBidragBatch(
    private val jobLauncher: JobLauncher,
    private val opprettForsendelseAldersjusteringerBidragJob: Job,
) {
    fun startOpprettForsendelseAldersjusteringBidragBatch() {
        jobLauncher.run(
            opprettForsendelseAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
