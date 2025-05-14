package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.opprett

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
class OpprettBrevAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun opprettBrevAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        opprettBrevAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("opprettBrevAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(opprettBrevAldersjusteringerBidragStep)
            .build()

    @Bean
    fun opprettBrevAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettBrevAldersjusteringerBidragBatchReader: OpprettBrevAldersjusteringerBidragBatchReader,
        opprettBrevAldersjusteringerBidragBatchProcessor: OpprettBrevAldersjusteringerBidragBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("opprettBrevAldersjusteringerBidragStep", jobRepository)
            .chunk<ForsendelseBestilling, Unit>(CHUNK_SIZE, transactionManager)
            .reader(opprettBrevAldersjusteringerBidragBatchReader)
            .processor(opprettBrevAldersjusteringerBidragBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
