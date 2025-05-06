package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.distribuer

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
class DistribuerBrevAldersjusteringerBidragBatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
    }

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
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        distribuerBrevAldersjusteringerBidragBatchReader: DistribuerBrevAldersjusteringerBidragBatchReader,
        distribuerBrevAldersjusteringerBidragBatchWriter: DistribuerBrevAldersjusteringerBidragBatchWriter,
    ): Step =
        StepBuilder("distribuerBrevAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(distribuerBrevAldersjusteringerBidragBatchReader)
            .writer(distribuerBrevAldersjusteringerBidragBatchWriter)
            .build()
}
