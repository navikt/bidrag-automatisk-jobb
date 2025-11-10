package no.nav.bidrag.automatiskjobb.batch.forsendelse.opprett

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
    fun startOpprettForsendelseAldersjusteringBidragBatch(prosesserFeilet: Boolean = false) {
        jobLauncher.run(
            opprettForsendelseAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addString("prosesserFeilet", prosesserFeilet.toString())
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
