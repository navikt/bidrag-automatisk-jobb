package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.oppgave

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
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
class OppgaveRevurderForskuddBatchConfiguration {
    @Bean
    fun oppgaveRevurderForskuddJob(
        jobRepository: JobRepository,
        oppgaveRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("oppgaveRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(oppgaveRevurderForskuddStep)
            .build()

    @Bean
    fun oppgaveRevurderForskuddStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        oppgaveRevurderForskuddBatchReader: RepositoryItemReader<RevurderingForskudd>,
        oppgaveRevurderForskuddBatchProcessor: OppgaveRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("oppgaveRevurderForskuddStep", jobRepository)
            .chunk<RevurderingForskudd, Int>(CHUNK_SIZE, transactionManager)
            .reader(oppgaveRevurderForskuddBatchReader)
            .processor(oppgaveRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun oppgaveRevurderForskuddBatchReader(
        revurderingForskuddRepository: RevurderingForskuddRepository,
    ): RepositoryItemReader<RevurderingForskudd> =
        RepositoryItemReaderBuilder<RevurderingForskudd>()
            .name("oppgaveRevurderForskuddBatchReader")
            .repository(revurderingForskuddRepository)
            .methodName("findAllByStatusIsAndBehandlingstypeIsAndOppgaveIsNull")
            .arguments(listOf(Status.BEHANDLET, Behandlingstype.MANUELL))
            .saveState(false)
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .build()
}
