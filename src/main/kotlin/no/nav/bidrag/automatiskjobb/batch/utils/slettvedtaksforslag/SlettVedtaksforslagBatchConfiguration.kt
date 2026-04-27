package no.nav.bidrag.automatiskjobb.batch.utils.slettvedtaksforslag

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
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
class SlettVedtaksforslagBatchConfiguration {
    @Bean
    fun slettVedtaksforslagJob(
        jobRepository: JobRepository,
        slettVedtaksforslagStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("slettVedtaksforslagJob", jobRepository)
            .listener(listener)
            .start(slettVedtaksforslagStep)
            .build()

    @Bean
    fun slettVedtaksforslagStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettVedtaksforslagBatchReader: SlettVedtaksforslagBatchReader,
        slettVedtaksforslagBatchProcessor: SlettVedtaksforslagBatchProcessor,
    ): Step =
        StepBuilder("slettVedtaksforslagStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(slettVedtaksforslagBatchReader)
            .processor(slettVedtaksforslagBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
