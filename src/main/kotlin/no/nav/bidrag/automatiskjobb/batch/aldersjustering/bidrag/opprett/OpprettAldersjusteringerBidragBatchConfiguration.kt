package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
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
class OpprettAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun opprettAldersjusteringerStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettAldersjusteringerBidragBatchReader: OpprettAldersjusteringerBidragBatchReader,
        opprettAldersjusteringerBidragBatchProcessor: OpprettAldersjusteringerBidragBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("opprettAldersjusteringerStep", jobRepository)
            .chunk<Barn, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(opprettAldersjusteringerBidragBatchReader)
            .processor(opprettAldersjusteringerBidragBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun opprettAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        opprettAldersjusteringerStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("opprettAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(opprettAldersjusteringerStep)
            .build()
}
