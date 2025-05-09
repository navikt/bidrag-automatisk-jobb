package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettallevedtaksforslag

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
class SlettAlleVedtaksforslagBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
    }

    @Bean
    fun slettAlleVedtaksforslagJob(
        jobRepository: JobRepository,
        slettAlleVedtaksforslagStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("slettAlleVedtaksforslagJob", jobRepository)
            .listener(listener)
            .start(slettAlleVedtaksforslagStep)
            .build()

    @Bean
    fun slettAlleVedtaksforslagStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettVedtaksforslagBatchReader: SlettAlleVedtaksforslagBatchReader,
        slettVedtaksforslagBatchWriter: SlettAlleVedtaksforslagBatchWriter,
    ): Step =
        StepBuilder("slettAlleVedtaksforslagStep", jobRepository)
            .chunk<List<Int>, List<Int>>(1, transactionManager)
            .reader(slettVedtaksforslagBatchReader)
            .writer(slettVedtaksforslagBatchWriter)
            .build()
}
