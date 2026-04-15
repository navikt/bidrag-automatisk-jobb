package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.DummyItemWriter
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
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
        listener: BatchListener,
    ): Job =
        JobBuilder("opprettAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(opprettAldersjusteringerStep)
            .build()
}
