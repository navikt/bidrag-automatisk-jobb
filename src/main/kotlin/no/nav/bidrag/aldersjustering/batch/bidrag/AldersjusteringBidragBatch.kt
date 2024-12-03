package no.nav.bidrag.aldersjustering.batch.bidrag

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class AldersjusteringBidragBatch(
    private val jobLauncher: JobLauncher,
    private val aldersjusteringBidragJob: Job,
) {
    fun startAldersjusteringBatch(
        forDato: YearMonth,
        kjøredato: LocalDate?,
    ) {
        jobLauncher.run(
            aldersjusteringBidragJob,
            JobParametersBuilder()
                .addLocalDate("forDato", forDato.atDay(1))
                .addLocalDate("kjøredato", kjøredato ?: LocalDate.now())
                .toJobParameters(),
        )
    }
}
