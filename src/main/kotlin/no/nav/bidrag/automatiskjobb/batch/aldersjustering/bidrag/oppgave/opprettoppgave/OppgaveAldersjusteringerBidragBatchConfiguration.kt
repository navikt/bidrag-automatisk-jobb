package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.opprettoppgave

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class OppgaveAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun oppgaveAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        oppgaveAldersjusteringerBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("oppgaveAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(oppgaveAldersjusteringerBidragStep)
            .build()

    @Bean
    fun oppgaveAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        oppgaveAldersjusteringerBidragBatchReader: OppgaveAldersjusteringerBidragBatchReader,
        oppgaveAldersjusteringerBidragBatchProcessor: OppgaveAldersjusteringerBidragBatchProcessor,
    ): Step =
        StepBuilder("oppgaveAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Int>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(oppgaveAldersjusteringerBidragBatchReader)
            .processor(oppgaveAldersjusteringerBidragBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
