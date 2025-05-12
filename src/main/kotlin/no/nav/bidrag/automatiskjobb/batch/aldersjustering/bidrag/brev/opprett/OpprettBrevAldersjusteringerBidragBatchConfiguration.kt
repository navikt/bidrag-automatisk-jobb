package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.opprett

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
class OpprettBrevAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
        const val GRID_SIZE = 5
    }

    @Bean
    fun opprettBrevAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        partitionedOpprettBrevAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("opprettBrevAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(partitionedOpprettBrevAldersjusteringerBidragStep)
            .build()

    @Bean
    fun opprettBrevAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettBrevAldersjusteringerBidragBatchReader: OpprettBrevAldersjusteringerBidragBatchReader,
        opprettBrevAldersjusteringerBidragBatchWriter: OpprettBrevAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("opprettBrevAldersjusteringerBidragStep", jobRepository)
            .chunk<ForsendelseBestilling, ForsendelseBestilling>(CHUNK_SIZE, transactionManager)
            .reader(opprettBrevAldersjusteringerBidragBatchReader)
            .writer(opprettBrevAldersjusteringerBidragBatchWriter)
            .build()

    @Bean
    fun partitionedOpprettBrevAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettBrevAldersjusteringerBidragStep: Step,
        moduloPartitioner: ModuloPartitioner,
    ): Step =
        StepBuilder("partitionedOpprettBrevAldersjusteringerBidragStep", jobRepository)
            .partitioner("opprettBrevAldersjusteringerBidragStep", moduloPartitioner)
            .step(opprettBrevAldersjusteringerBidragStep)
            .gridSize(GRID_SIZE)
            .taskExecutor(SimpleAsyncTaskExecutor())
            .build()
}
