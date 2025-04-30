package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.partition.support.SimplePartitioner
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
    ): Step =
        StepBuilder("partitionedStep", jobRepository)
            .partitioner("fattVedtakOmAldersjusteringerBidragStep", SimplePartitioner())
            .step(fattVedtakOmAldersjusteringerBidragStep)
            .gridSize(5)
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
            .writer(fattVedtakOmAldersjusteringerBidragBatchWriter)
            .build()
}
