package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.opprett

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
class OpprettBrevAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
    }

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
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettBrevAldersjusteringerBidragBatchReader: OpprettBrevAldersjusteringerBidragBatchReader,
        opprettBrevAldersjusteringerBidragBatchWriter: OpprettBrevAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("opprettBrevAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(opprettBrevAldersjusteringerBidragBatchReader)
            .writer(opprettBrevAldersjusteringerBidragBatchWriter)
            .build()
}
