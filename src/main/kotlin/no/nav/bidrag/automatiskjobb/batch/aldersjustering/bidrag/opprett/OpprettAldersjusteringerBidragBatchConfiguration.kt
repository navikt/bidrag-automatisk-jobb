package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class OpprettAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
        const val GRID_SIZE = 10
    }

    @Bean
    fun opprettAldersjusteringExecutor(): TaskExecutor? =
        SimpleAsyncTaskExecutor("opprett_aldersjustering").apply {
            concurrencyLimit = GRID_SIZE
        }

    @Bean
    fun opprettAldersjusteringerStep(
        @Qualifier("opprettAldersjusteringExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettAldersjusteringerBidragBatchReader: OpprettAldersjusteringerBidragBatchReader,
        opprettAldersjusteringerBidragBatchWriter: OpprettAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("opprettAldersjusteringerStep", jobRepository)
            .chunk<Barn, Barn>(CHUNK_SIZE, transactionManager)
            .reader(opprettAldersjusteringerBidragBatchReader)
            .writer(opprettAldersjusteringerBidragBatchWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun opprettAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        opprettAldersjusteringerStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("opprettAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(opprettAldersjusteringerStep)
            .build()
}
