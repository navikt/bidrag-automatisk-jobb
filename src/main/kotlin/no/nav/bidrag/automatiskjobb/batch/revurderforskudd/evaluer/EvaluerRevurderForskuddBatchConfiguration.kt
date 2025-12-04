package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.data.domain.Sort
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
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("evaluerRevurderForskuddStep", jobRepository)
            .chunk<RevurderingForskudd, Unit>(CHUNK_SIZE, transactionManager)
            .reader(evaluerRevurderForskuddBatchReader)
            .processor(evaluerRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun evaluerRevurderForskuddBatchReader(
        revurderForskuddRepository: RevurderForskuddRepository,
    ): RepositoryItemReader<RevurderingForskudd> =
        RepositoryItemReaderBuilder<RevurderingForskudd>()
            .name("evaluerRevurderForskuddBatchReader")
            .repository(revurderForskuddRepository)
            .methodName("findAllByStatusIs")
            .arguments(listOf(Status.UBEHANDLET))
            .saveState(false)
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .build()
}
