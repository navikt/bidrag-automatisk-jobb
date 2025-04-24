package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class OpprettAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
    }

    @Bean
    fun opprettAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        aldersjusteringBidragOpprettAldersjusteringerStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("opprettAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(aldersjusteringBidragOpprettAldersjusteringerStep)
            .build()

    @Bean
    fun opprettAldersjusteringerBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        aldersjusteringBidragOpprettAldersjusteringerBatchReader: OpprettAldersjusteringerBidragBatchReader,
        opprettAldersjusteringerBidragBatchWriter: OpprettAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("opprettAldersjusteringerBidragStep", jobRepository)
            .chunk<Barn, Barn>(CHUNK_SIZE, transactionManager)
            .reader(aldersjusteringBidragOpprettAldersjusteringerBatchReader)
            .writer(opprettAldersjusteringerBidragBatchWriter)
            .build()
}
