package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.reset

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultat
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class ResetAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun resetAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        resetAldersjusteringerBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("resetAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(resetAldersjusteringerBidragStep)
            .build()

    @Bean
    fun resetAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        resetAldersjusteringerBidragStep: ResetAldersjusteringerBidragBatchReader,
        resetAldersjusteringerBidragBatchProcessor: ResetAldersjusteringerBidragBatchProcessor,
    ): Step =
        StepBuilder("resetAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Boolean>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(resetAldersjusteringerBidragStep)
            .processor(resetAldersjusteringerBidragBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
