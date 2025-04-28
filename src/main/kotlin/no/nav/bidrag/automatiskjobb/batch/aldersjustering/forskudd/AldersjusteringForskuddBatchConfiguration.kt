package no.nav.bidrag.automatiskjobb.batch.aldersjustering.forskudd

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.RepositoryItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class AldersjusteringForskuddBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
    }

    @Bean
    fun aldersjusteringForskuddJob(
        jobRepository: JobRepository,
        aldersjusteringForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("aldersjusteringForskuddJob", jobRepository)
            .listener(listener)
            .start(aldersjusteringForskuddStep)
            .build()

    @Bean
    fun aldersjusteringForskuddStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        aldersjusteringForskuddBatchReader: RepositoryItemReader<Barn>,
        aldersjusteringForskuddBatchProcessor: AldersjusteringForskuddBatchProcessor,
        aldersjusteringBatchWriter: RepositoryItemWriter<Barn>,
    ): Step =
        StepBuilder("aldersjusteringForskuddStep", jobRepository)
            .chunk<Barn, Barn>(CHUNK_SIZE, transactionManager)
            .reader(aldersjusteringForskuddBatchReader)
            .processor(aldersjusteringForskuddBatchProcessor)
            .writer(aldersjusteringBatchWriter)
            .build()
}
