package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett.OpprettAldersjusteringerBidragBatchConfiguration
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
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
class BeregnAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 500
        const val GRID_SIZE = 10
    }

    @Bean
    fun beregnAldersjusteringExecutor(): TaskExecutor? =
        SimpleAsyncTaskExecutor("beregn_aldersjustering").apply {
            concurrencyLimit = OpprettAldersjusteringerBidragBatchConfiguration.GRID_SIZE
        }

    @Bean
    fun beregnAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        beregnAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("beregnAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(beregnAldersjusteringerBidragStep)
            .build()

    @Bean
    fun beregnAldersjusteringerBidragStep(
        @Qualifier("beregnAldersjusteringExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        beregnAldersjusteringerBidragBatchReader: BeregnAldersjusteringerBidragBatchReader,
        beregnAldersjusteringerBidragBatchWriter: BeregnAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("beregnAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(beregnAldersjusteringerBidragBatchReader)
            .writer(beregnAldersjusteringerBidragBatchWriter)
            .taskExecutor(taskExecutor)
            .build()
}
