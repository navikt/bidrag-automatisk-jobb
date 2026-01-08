package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class EvaluerRevurderForskuddBatchConfiguration {
    @Bean
    fun evaluerRevurderForskuddJob(
        jobRepository: JobRepository,
        evaluerRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("evaluerRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(evaluerRevurderForskuddStep)
            .build()

    @Bean
    fun evaluerRevurderForskuddStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        evaluerRevurderForskuddBatchReader: RepositoryItemReader<RevurderingForskudd>,
        evaluerRevurderForskuddBatchProcessor: EvaluerRevurderForskuddBatchProcessor,
        evaluerRevurderForskuddBatchWriter: EvaluerRevurderForskuddBatchWriter,
    ): Step =
        StepBuilder("evaluerRevurderForskuddStep", jobRepository)
            .chunk<RevurderingForskudd, RevurderingForskudd>(CHUNK_SIZE, transactionManager)
            .reader(evaluerRevurderForskuddBatchReader)
            .processor(evaluerRevurderForskuddBatchProcessor)
            .writer(evaluerRevurderForskuddBatchWriter)
            .taskExecutor(taskExecutor)
            .faultTolerant()
            .skipLimit(CHUNK_SIZE)
            .build()
}
