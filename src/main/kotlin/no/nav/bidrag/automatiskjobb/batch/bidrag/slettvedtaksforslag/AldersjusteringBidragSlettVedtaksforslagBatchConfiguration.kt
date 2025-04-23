package no.nav.bidrag.automatiskjobb.batch.bidrag.slettvedtaksforslag

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class AldersjusteringBidragSlettVedtaksforslagBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
    }

    @Bean
    fun aldersjusteringBidragSlettVedtaksforslagJob(
        jobRepository: JobRepository,
        aldersjusteringBidragSlettVedtaksforslagStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("aldersjusteringBidragSlettVedtaksforslagJob", jobRepository)
            .listener(listener)
            .start(aldersjusteringBidragSlettVedtaksforslagStep)
            .build()

    @Bean
    fun aldersjusteringBidragSlettVedtaksforslagStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        aldersjusteringBidragSlettVedtaksforslagBatchReader: AldersjusteringBidragSlettVedtaksforslagBatchReader,
        aldersjusteringBidragSlettVedtaksforslagBatchWriter: AldersjusteringBidragSlettVedtaksforslagBatchWriter,
    ): Step =
        StepBuilder("aldersjusteringBidragSlettVedtaksforslagStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(aldersjusteringBidragSlettVedtaksforslagBatchReader)
            .writer(aldersjusteringBidragSlettVedtaksforslagBatchWriter)
            .build()
}
