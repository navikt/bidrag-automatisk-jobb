package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.slettoppgave

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
class SlettOppgaveAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun slettOppgaveAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        slettOppgaveAldersjusteringerBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("slettOppgaveAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(slettOppgaveAldersjusteringerBidragStep)
            .build()

    @Bean
    fun slettOppgaveAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettOppgaveAldersjusteringerBidragBatchReader: SlettOppgaveAldersjusteringerBidragBatchReader,
        slettOppgaveAldersjusteringerBidragBatchProcessor: SlettOppgaveAldersjusteringerBidragBatchProcessor,
    ): Step =
        StepBuilder("slettOppgaveAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Int>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(slettOppgaveAldersjusteringerBidragBatchReader)
            .processor(slettOppgaveAldersjusteringerBidragBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
