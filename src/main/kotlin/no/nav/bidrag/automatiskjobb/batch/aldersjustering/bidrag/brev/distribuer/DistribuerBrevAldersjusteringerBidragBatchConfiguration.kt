package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.distribuer

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
class DistribuerBrevAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun distribuerBrevAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        distribuerBrevAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("distribuerBrevAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(distribuerBrevAldersjusteringerBidragStep)
            .build()

    @Bean
    fun distribuerBrevAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        distribuerBrevAldersjusteringerBidragBatchReader: DistribuerBrevAldersjusteringerBidragBatchReader,
        distribuerBrevAldersjusteringerBidragBatchProcessor: DistribuerBrevAldersjusteringerBidragBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("distribuerBrevAldersjusteringerBidragStep", jobRepository)
            .chunk<ForsendelseBestilling, Unit>(CHUNK_SIZE, transactionManager)
            .reader(distribuerBrevAldersjusteringerBidragBatchReader)
            .processor(distribuerBrevAldersjusteringerBidragBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
