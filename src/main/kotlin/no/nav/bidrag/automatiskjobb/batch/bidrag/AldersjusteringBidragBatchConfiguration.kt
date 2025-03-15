package no.nav.bidrag.automatiskjobb.batch.bidrag

import no.nav.bidrag.automatiskjobb.batch.AldersjusteringJobCompletionNotificationListener
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
class AldersjusteringBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
    }

    @Bean
    fun aldersjusteringBidragJob(
        jobRepository: JobRepository,
        aldersjusteringBidragStep: Step,
        listener: AldersjusteringJobCompletionNotificationListener,
    ): Job =
        JobBuilder("aldersjusteringBidragJob", jobRepository)
            .listener(listener)
            .start(aldersjusteringBidragStep)
            .build()

    @Bean
    fun aldersjusteringBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        aldersjusteringBidragBatchReader: RepositoryItemReader<Barn>,
        aldersjusteringBidragBatchProcessor: AldersjusteringBidragBatchProcessor,
        aldersjusteringBatchWriter: RepositoryItemWriter<Barn>,
    ): Step =
        StepBuilder("aldersjusteringBidragStep", jobRepository)
            .chunk<Barn, Barn>(CHUNK_SIZE, transactionManager)
            .reader(aldersjusteringBidragBatchReader)
            .processor(aldersjusteringBidragBatchProcessor)
            .writer(aldersjusteringBatchWriter)
            .build()
}
