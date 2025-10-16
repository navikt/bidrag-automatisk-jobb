package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.forsendelse

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
class ForsendelseRevurderForskuddBatchConfiguration {
    @Bean
    fun forsendelseRevurderForskuddJob(
        jobRepository: JobRepository,
        forsendelseRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("forsendelseRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(forsendelseRevurderForskuddStep)
            .build()

    @Bean
    fun forsendelseRevurderForskuddStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        forsendelseRevurderForskuddBatchReader: ForsendelseRevurderForskuddBatchReader,
        forsendelseRevurderForskuddBatchProcessor: ForsendelseRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("forsendelseRevurderForskuddStep", jobRepository)
            .chunk<Any, Unit>(CHUNK_SIZE, transactionManager)
            .reader(forsendelseRevurderForskuddBatchReader)
            .processor(forsendelseRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .taskExecutor(taskExecutor)
            .build()
}
