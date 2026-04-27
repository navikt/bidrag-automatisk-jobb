package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

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
class FattVedtakOmAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun fattVedtakOmAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        fattVedtakOmAldersjusteringerBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("fattVedtakOmAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(fattVedtakOmAldersjusteringerBidragStep)
            .build()

    @Bean
    fun fattVedtakOmAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        fattVedtakOmAldersjusteringerBidragBatchReader: FattVedtakOmAldersjusteringerBidragBatchReader,
        fattVedtakOmAldersjusteringerBidragBatchProcessor: FattVedtakOmAldersjusteringerBidragBatchProcessor,
    ): Step =
        StepBuilder("fattVedtakOmAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Unit>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(fattVedtakOmAldersjusteringerBidragBatchReader)
            .processor(fattVedtakOmAldersjusteringerBidragBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
