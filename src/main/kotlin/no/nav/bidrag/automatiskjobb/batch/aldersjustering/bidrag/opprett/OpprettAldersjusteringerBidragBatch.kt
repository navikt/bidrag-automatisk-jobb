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
        aldersjusteringsdato: LocalDate?,
        år: Long,
    ) {
        jobLauncher.run(
            opprettAldersjusteringerBidragJob,
            JobParametersBuilder()
                .addLocalDate(
                    "aldersjusteringsdato",
                    aldersjusteringsdato ?: LocalDate
                        .now()
                        .withYear(år.toInt())
                        .withMonth(7)
                        .withDayOfMonth(1),
                ).addLong("år", år)
                .addString("batchId", "aldersjustering_bidrag_$år")
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
