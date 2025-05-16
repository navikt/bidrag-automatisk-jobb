package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.opprett

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
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
class OpprettForsendelseAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun opprettForsendelseAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        opprettForsendelseAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("opprettForsendelseAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(opprettForsendelseAldersjusteringerBidragStep)
            .build()

    @Bean
    fun opprettForsendelseAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettForsendelseAldersjusteringerBidragBatchReader: OpprettForsendelseAldersjusteringerBidragBatchReader,
        opprettForsendelseAldersjusteringerBidragBatchProcessor: OpprettForsendelseAldersjusteringerBidragBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("opprettForsendelseAldersjusteringerBidragStep", jobRepository)
            .chunk<ForsendelseBestilling, Unit>(CHUNK_SIZE, transactionManager)
            .reader(opprettForsendelseAldersjusteringerBidragBatchReader)
            .processor(opprettForsendelseAldersjusteringerBidragBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
