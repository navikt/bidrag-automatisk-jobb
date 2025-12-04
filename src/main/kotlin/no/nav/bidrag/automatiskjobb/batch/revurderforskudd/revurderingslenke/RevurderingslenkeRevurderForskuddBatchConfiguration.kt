package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke

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
class RevurderingslenkeRevurderForskuddBatchConfiguration {
    @Bean
    fun revurderingslenkeRevurderForskuddJob(
        jobRepository: JobRepository,
        revurderingslenkeRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("revurderingslenkeRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(revurderingslenkeRevurderForskuddStep)
            .build()

    @Bean
    fun revurderingslenkeRevurderForskuddStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        revurderingslenkeRevurderForskuddBatchReader: RepositoryItemReader<RevurderingForskudd>,
        revurderingslenkeRevurderForskuddBatchProcessor: RevurderingslenkeRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("revurderingslenkeRevurderForskuddStep", jobRepository)
            .chunk<RevurderingForskudd, Int?>(CHUNK_SIZE, transactionManager)
            .reader(revurderingslenkeRevurderForskuddBatchReader)
            .processor(revurderingslenkeRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun revurderingslenkeRevurderForskuddBatchReader(
        revurderForskuddRepository: RevurderForskuddRepository,
    ): RepositoryItemReader<RevurderingForskudd> =
        RepositoryItemReaderBuilder<RevurderingForskudd>()
            .name("revurderingslenkeRevurderForskuddBatchReader")
            .repository(revurderForskuddRepository)
            .methodName("findAllByStatusIsAndVurdereTilbakekrevingIsTrueAndOppgaveIsNull")
            .arguments(listOf(Status.FATTET))
            .saveState(false)
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .build()
}
