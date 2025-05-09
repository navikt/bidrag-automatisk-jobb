package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

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
class FattVedtakOmAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
        const val GRID_SIZE = 5
    }

    @Bean
    fun fattVedtakOmAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        partitionedFattVedtakOmAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("fattVedtakOmAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(partitionedFattVedtakOmAldersjusteringerBidragStep)
            .build()

    @Bean
    fun partitionedFattVedtakOmAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        fattVedtakOmAldersjusteringerBidragStep: Step,
        moduloPartitioner: ModuloPartitioner,
    ): Step =
        StepBuilder("partitionedStep", jobRepository)
            .partitioner("fattVedtakOmAldersjusteringerBidragStep", moduloPartitioner)
            .step(fattVedtakOmAldersjusteringerBidragStep)
            .gridSize(GRID_SIZE)
            .taskExecutor(SimpleAsyncTaskExecutor())
            .build()

    @Bean
    fun fattVedtakOmAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        fattVedtakOmAldersjusteringerBidragBatchReader: FattVedtakOmAldersjusteringerBidragBatchReader,
        fattVedtakOmAldersjusteringerBidragBatchWriter: FattVedtakOmAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("fattVedtakOmAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(fattVedtakOmAldersjusteringerBidragBatchReader)
            .processor(fattVedtakOmAldersjusteringerBidragBatchReader)
            .writer(fattVedtakOmAldersjusteringerBidragBatchWriter)
            .build()
}
