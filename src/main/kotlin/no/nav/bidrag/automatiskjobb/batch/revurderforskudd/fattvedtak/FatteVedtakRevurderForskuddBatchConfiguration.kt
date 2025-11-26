package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak

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
        fatteVedtakRevurderForskuddBatchReader: RepositoryItemReader<RevurderingForskudd>,
        fatteVedtakRevurderForskuddBatchProcessor: FatteVedtakRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("fatteVedtakRevurderForskuddStep", jobRepository)
            .chunk<RevurderingForskudd, Unit>(CHUNK_SIZE, transactionManager)
            .reader(fatteVedtakRevurderForskuddBatchReader)
            .processor(fatteVedtakRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun fatteVedtakRevurderForskuddBatchReader(
        revurderForskuddRepository: RevurderForskuddRepository,
    ): RepositoryItemReader<RevurderingForskudd> =
        RepositoryItemReaderBuilder<RevurderingForskudd>()
            .name("fatteVedtakRevurderForskuddBatchReader")
            .repository(revurderForskuddRepository)
            .methodName("findAllByStatusIsAndVedtakIsNotNull")
            .arguments(listOf(Status.BEHANDLET))
            .saveState(false)
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .build()
}
