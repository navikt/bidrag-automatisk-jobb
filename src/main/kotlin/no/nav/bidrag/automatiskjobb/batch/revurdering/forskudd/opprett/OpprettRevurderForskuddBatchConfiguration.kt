package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.opprett

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
class OpprettRevurderForskuddBatchConfiguration {
    @Bean
    fun opprettRevurderForskuddJob(
        jobRepository: JobRepository,
        opprettRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("opprettRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(opprettRevurderForskuddStep)
            .build()

    @Bean
    fun opprettRevurderForskuddStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettRevurderForskuddBatchReader: OpprettRevurderForskuddBatchReader,
        opprettRevurderForskuddBatchProcessor: OpprettRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("opprettRevurderForskuddStep", jobRepository)
            .chunk<Any, Unit>(CHUNK_SIZE, transactionManager)
            .reader(opprettRevurderForskuddBatchReader)
            .processor(opprettRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .taskExecutor(taskExecutor)
            .build()
}
