package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class FattVedtakOmAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun fattVedtakOmAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        fattVedtakOmAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("fattVedtakOmAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(fattVedtakOmAldersjusteringerBidragStep)
            .build()

    @Bean
    fun fattVedtakOmAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        fattVedtakOmAldersjusteringerBidragBatchReader: FattVedtakOmAldersjusteringerBidragBatchReader,
        fattVedtakOmAldersjusteringerBidragBatchProcessor: FattVedtakOmAldersjusteringerBidragBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("fattVedtakOmAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Unit>(CHUNK_SIZE, transactionManager)
            .reader(fattVedtakOmAldersjusteringerBidragBatchReader)
            .processor(fattVedtakOmAldersjusteringerBidragBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
