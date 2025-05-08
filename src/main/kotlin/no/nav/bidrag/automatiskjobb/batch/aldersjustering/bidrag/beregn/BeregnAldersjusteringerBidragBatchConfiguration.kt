package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

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
class BeregnAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
        const val GRID_SIZE = 5
    }

    @Bean
    fun beregnAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        partitionedBeregnAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("beregnAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(partitionedBeregnAldersjusteringerBidragStep)
            .build()

    @Bean
    fun beregnAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        beregnAldersjusteringerBidragBatchReader: BeregnAldersjusteringerBidragBatchReader,
        beregnAldersjusteringerBidragBatchWriter: BeregnAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("beregnAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(beregnAldersjusteringerBidragBatchReader)
            .processor(beregnAldersjusteringerBidragBatchReader) // Use reader as processor too
            .writer(beregnAldersjusteringerBidragBatchWriter)
            .build()

    @Bean
    fun partitionedBeregnAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        beregnAldersjusteringerBidragStep: Step,
        moduloPartitioner: ModuloPartitioner,
    ): Step =
        StepBuilder("partitionedStep", jobRepository)
            .partitioner("beregnAldersjusteringerBidragStep", moduloPartitioner)
            .step(beregnAldersjusteringerBidragStep)
            .gridSize(GRID_SIZE)
            .taskExecutor(SimpleAsyncTaskExecutor())
            .build()
}
