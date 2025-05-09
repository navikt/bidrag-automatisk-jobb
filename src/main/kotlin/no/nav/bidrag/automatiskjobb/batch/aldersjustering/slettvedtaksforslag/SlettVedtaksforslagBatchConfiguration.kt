package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.common.ModuloPartitioner
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class SlettVedtaksforslagBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
        const val GRID_SIZE = 5
    }

    @Bean
    fun slettVedtaksforslagJob(
        jobRepository: JobRepository,
        slettVedtaksforslagStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("slettVedtaksforslagJob", jobRepository)
            .listener(listener)
            .start(slettVedtaksforslagStep)
            .build()

    @Bean
    fun partitionedSlettVedtaksforslagStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettVedtaksforslagStep: Step,
        moduloPartitioner: ModuloPartitioner,
    ): Step =
        StepBuilder("partitionedSlettVedtaksforslagStep", jobRepository)
            .partitioner("slettVedtaksforslagStep", moduloPartitioner)
            .step(slettVedtaksforslagStep)
            .gridSize(GRID_SIZE)
            .taskExecutor(SimpleAsyncTaskExecutor())
            .build()

    @Bean
    fun slettVedtaksforslagStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettVedtaksforslagBatchReader: SlettVedtaksforslagBatchReader,
        slettVedtaksforslagBatchWriter: SlettVedtaksforslagBatchWriter,
    ): Step =
        StepBuilder("slettVedtaksforslagStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(slettVedtaksforslagBatchReader)
            .processor(slettVedtaksforslagBatchReader)
            .writer(slettVedtaksforslagBatchWriter)
            .build()
}
