package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.slettoppgave

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
class SlettOppgaveAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun slettOppgaveAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        slettOppgaveAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("slettOppgaveAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(slettOppgaveAldersjusteringerBidragStep)
            .build()

    @Bean
    fun slettOppgaveAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettOppgaveAldersjusteringerBidragBatchReader: SlettOppgaveAldersjusteringerBidragBatchReader,
        slettOppgaveAldersjusteringerBidragBatchProcessor: SlettOppgaveAldersjusteringerBidragBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("slettOppgaveAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Int?>(CHUNK_SIZE, transactionManager)
            .reader(slettOppgaveAldersjusteringerBidragBatchReader)
            .processor(slettOppgaveAldersjusteringerBidragBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
