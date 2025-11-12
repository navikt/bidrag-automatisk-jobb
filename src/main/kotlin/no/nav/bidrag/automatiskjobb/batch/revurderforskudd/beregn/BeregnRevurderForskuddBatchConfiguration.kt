package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.beregn

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderingForskuddRepository
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
class BeregnRevurderForskuddBatchConfiguration {
    @Bean
    fun beregnRevurderForskuddJob(
        jobRepository: JobRepository,
        beregnRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("beregnRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(beregnRevurderForskuddStep)
            .build()

    @Bean
    fun beregnRevurderForskuddStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        beregnRevurderForskuddBatchReader: RepositoryItemReader<RevurderingForskudd>,
        beregnRevurderForskuddBatchProcessor: BeregnRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("beregnRevurderForskuddStep", jobRepository)
            .chunk<RevurderingForskudd, Unit>(CHUNK_SIZE, transactionManager)
            .reader(beregnRevurderForskuddBatchReader)
            .processor(beregnRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun beregnRevurderForskuddBatchReader(
        revurderingForskuddRepository: RevurderingForskuddRepository,
    ): RepositoryItemReader<RevurderingForskudd> =
        RepositoryItemReaderBuilder<RevurderingForskudd>()
            .name("beregnRevurderForskuddBatchReader")
            .repository(revurderingForskuddRepository)
            .methodName("findAllByStatusIs")
            .arguments(listOf(Status.UBEHANDLET))
            .saveState(false)
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .build()
}
