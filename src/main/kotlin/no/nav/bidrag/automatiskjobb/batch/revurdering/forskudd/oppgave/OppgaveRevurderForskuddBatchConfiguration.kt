package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.oppgave

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        oppgaveRevurderForskuddBatchReader: OppgaveRevurderForskuddBatchReader,
        oppgaveRevurderForskuddBatchProcessor: OppgaveRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("oppgaveRevurderForskuddStep", jobRepository)
            .chunk<Any, Unit>(CHUNK_SIZE, transactionManager)
            .reader(oppgaveRevurderForskuddBatchReader)
            .processor(oppgaveRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .build()
}
