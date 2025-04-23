package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprettaldersjustering

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class AldersjusteringBidragOpprettAldersjusteringerBatch(
    private val jobLauncher: JobLauncher,
    private val aldersjusteringBidragOpprettAldersjusteringerJob: Job,
) {
    fun startAldersjusteringBidragOpprettAldersjusteringBatch(
        kjøredato: LocalDate?,
        år: Long,
    ) {
        jobLauncher.run(
            aldersjusteringBidragOpprettAldersjusteringerJob,
            JobParametersBuilder()
                .addLocalDate("kjøredato", kjøredato ?: LocalDate.now())
                .addLong("år", år)
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
