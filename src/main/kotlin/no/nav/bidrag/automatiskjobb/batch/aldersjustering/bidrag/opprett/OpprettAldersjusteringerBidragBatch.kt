package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class OpprettAldersjusteringerBidragBatch(
    private val jobLauncher: JobLauncher,
    private val opprettAldersjusteringerBidragJob: Job,
) {
    fun startOpprettAldersjusteringBidragBatch(
        kjøredato: LocalDate?,
        år: Long,
    ) {
        jobLauncher.run(
            opprettAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addLocalDate("kjøredato", kjøredato ?: LocalDate.now())
                .addLong("år", år)
                .addString("runId", "aldersjustering_bidrag_${UUID.randomUUID()}")
                .toJobParameters(),
        )
    }
}
