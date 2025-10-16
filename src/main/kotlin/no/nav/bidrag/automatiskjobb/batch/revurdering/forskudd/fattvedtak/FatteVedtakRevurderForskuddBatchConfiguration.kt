package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.fattvedtak

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
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
class FatteVedtakRevurderForskuddBatchConfiguration {
    @Bean
    fun fatteVedtakRevurderForskuddJob(
        jobRepository: JobRepository,
        fatteVedtakRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("fatteVedtakRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(fatteVedtakRevurderForskuddStep)
            .build()

    @Bean
    fun fatteVedtakRevurderForskuddStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        fatteVedtakRevurderForskuddBatchReader: FatteVedtakRevurderForskuddBatchReader,
        fatteVedtakRevurderForskuddBatchProcessor: FatteVedtakRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("fatteVedtakRevurderForskuddStep", jobRepository)
            .chunk<Any, Unit>(CHUNK_SIZE, transactionManager)
            .reader(fatteVedtakRevurderForskuddBatchReader)
            .processor(fatteVedtakRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .taskExecutor(taskExecutor)
            .build()
}
