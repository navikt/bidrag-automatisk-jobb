package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak.FattVedtakOmAldersjusteringerBidragBatchReader
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak.FattVedtakOmAldersjusteringerBidragBatchWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.partition.support.SimplePartitioner
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class OppgaveAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
    }

    @Bean
    fun oppgaveAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        partitionedOppgaveAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("oppgaveAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(partitionedOppgaveAldersjusteringerBidragStep)
            .build()

    @Bean
    fun partitionedOppgaveAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettAldersjusteringerBidragStep: Step,
    ): Step =
        StepBuilder("partitionedStep", jobRepository)
            .partitioner("oppgaveAldersjusteringerBidragStep", SimplePartitioner())
            .step(opprettAldersjusteringerBidragStep)
            .gridSize(5)
            .taskExecutor(SimpleAsyncTaskExecutor())
            .build()

    @Bean
    fun oppgaveAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        fattVedtakOmAldersjusteringerBidragBatchReader: FattVedtakOmAldersjusteringerBidragBatchReader,
        fattVedtakOmAldersjusteringerBidragBatchWriter: FattVedtakOmAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("oppgaveAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(fattVedtakOmAldersjusteringerBidragBatchReader)
            .writer(fattVedtakOmAldersjusteringerBidragBatchWriter)
            .build()
}
