package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.distribuer

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.common.ModuloPartitioner
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
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
class DistribuerBrevAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
        const val GRID_SIZE = 5
    }

    @Bean
    fun distribuerBrevAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        partitionedDistribuerBrevAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("distribuerBrevAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(partitionedDistribuerBrevAldersjusteringerBidragStep)
            .build()

    @Bean
    fun distribuerBrevAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        distribuerBrevAldersjusteringerBidragBatchReader: DistribuerBrevAldersjusteringerBidragBatchReader,
        distribuerBrevAldersjusteringerBidragBatchWriter: DistribuerBrevAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("distribuerBrevAldersjusteringerBidragStep", jobRepository)
            .chunk<ForsendelseBestilling, ForsendelseBestilling>(CHUNK_SIZE, transactionManager)
            .reader(distribuerBrevAldersjusteringerBidragBatchReader)
            .processor(distribuerBrevAldersjusteringerBidragBatchReader)
            .writer(distribuerBrevAldersjusteringerBidragBatchWriter)
            .build()

    @Bean
    fun partitionedDistribuerBrevAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        distribuerBrevAldersjusteringerBidragStep: Step,
        moduloPartitioner: ModuloPartitioner,
    ): Step =
        StepBuilder("partitionedDistribuerBrevAldersjusteringerBidragStep", jobRepository)
            .partitioner("distribuerBrevAldersjusteringerBidragStep", moduloPartitioner)
            .step(distribuerBrevAldersjusteringerBidragStep)
            .gridSize(GRID_SIZE)
            .taskExecutor(SimpleAsyncTaskExecutor())
            .build()
}
