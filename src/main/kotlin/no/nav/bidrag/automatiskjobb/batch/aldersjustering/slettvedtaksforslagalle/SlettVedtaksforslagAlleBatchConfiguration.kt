package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslagalle

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class SlettVedtaksforslagAlleBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
    }

    @Bean
    fun slettVedtaksforslagAlleJob(
        jobRepository: JobRepository,
        slettVedtaksforslagAlleStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("slettVedtaksforslagAlleJob", jobRepository)
            .listener(listener)
            .start(slettVedtaksforslagAlleStep)
            .build()

    @Bean
    fun slettVedtaksforslagAlleStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettVedtaksforslagBatchReader: SlettVedtaksforslagAlleBatchReader,
        slettVedtaksforslagBatchWriter: SlettVedtaksforslagAlleBatchWriter,
    ): Step =
        StepBuilder("slettVedtaksforslagAlleStep", jobRepository)
            .chunk<List<Int>, List<Int>>(1, transactionManager)
            .reader(slettVedtaksforslagBatchReader)
            .writer(slettVedtaksforslagBatchWriter)
            .build()
}
