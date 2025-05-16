package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.slett

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SlettForsendelsrSomSkalSlettesBidragBatch(
    private val jobLauncher: JobLauncher,
    private val slettForsendelserSomSkalSlettesJob: Job,
) {
    fun startSlettForsendelserSomSkalSlettesBatch() {
        jobLauncher.run(
            slettForsendelserSomSkalSlettesJob,
            JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
