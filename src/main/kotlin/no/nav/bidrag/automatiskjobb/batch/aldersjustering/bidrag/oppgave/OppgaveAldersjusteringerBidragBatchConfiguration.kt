package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
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
class OppgaveAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun oppgaveAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        oppgaveAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("oppgaveAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(oppgaveAldersjusteringerBidragStep)
            .build()

    @Bean
    fun oppgaveAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        oppgaveAldersjusteringerBidragBatchReader: OppgaveAldersjusteringerBidragBatchReader,
        oppgaveAldersjusteringerBidragBatchProcessor: OppgaveAldersjusteringerBidragBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("oppgaveAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Int?>(CHUNK_SIZE, transactionManager)
            .reader(oppgaveAldersjusteringerBidragBatchReader)
            .processor(oppgaveAldersjusteringerBidragBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
