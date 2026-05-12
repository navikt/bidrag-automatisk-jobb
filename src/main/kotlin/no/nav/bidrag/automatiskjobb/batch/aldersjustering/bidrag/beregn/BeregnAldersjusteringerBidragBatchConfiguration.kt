package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultat
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
class BeregnAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun beregnAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        beregnAldersjusteringerBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("beregnAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(beregnAldersjusteringerBidragStep)
            .build()

    @Bean
    fun beregnAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        beregnAldersjusteringerBidragBatchReader: BeregnAldersjusteringerBidragBatchReader,
        beregnAldersjusteringerBidragBatchProcessor: BeregnAldersjusteringerBidragBatchProcessor,
    ): Step =
        StepBuilder("beregnAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, AldersjusteringResultat>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(beregnAldersjusteringerBidragBatchReader)
            .processor(beregnAldersjusteringerBidragBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
