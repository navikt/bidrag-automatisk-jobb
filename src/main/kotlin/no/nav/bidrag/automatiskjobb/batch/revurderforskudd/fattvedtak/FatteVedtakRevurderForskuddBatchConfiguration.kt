package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class FatteVedtakRevurderForskuddBatchConfiguration {
    @Bean
    fun fatteVedtakRevurderForskuddJob(
        jobRepository: JobRepository,
        fatteVedtakRevurderForskuddStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("fatteVedtakRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(fatteVedtakRevurderForskuddStep)
            .build()

    @Bean
    fun fatteVedtakRevurderForskuddStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        fatteVedtakRevurderForskuddBatchReader: RepositoryItemReader<RevurderingForskudd>,
        fatteVedtakRevurderForskuddBatchProcessor: FatteVedtakRevurderForskuddBatchProcessor,
    ): Step =
        StepBuilder("fatteVedtakRevurderForskuddStep", jobRepository)
            .chunk<RevurderingForskudd, Unit>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(fatteVedtakRevurderForskuddBatchReader)
            .processor(fatteVedtakRevurderForskuddBatchProcessor)
            .writer { }
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
