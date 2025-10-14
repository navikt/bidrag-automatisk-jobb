package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.beregn

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BeregnRevurderForskuddBatchConfiguration {
    @Bean
    fun beregnRevurderForskuddJob(
        jobRepository: JobRepository,
        beregnRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("beregnRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(beregnRevurderForskuddStep)
            .build()

    @Bean
    fun beregnRevurderForskuddStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        beregnRevurderForskuddBatchReader: BeregnRevurderForskuddBatchReader,
        beregnRevurderForskuddBatchProcessor: BeregnRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("beregnRevurderForskuddStep", jobRepository)
            .chunk<Any, Unit>(CHUNK_SIZE, transactionManager)
            .reader(beregnRevurderForskuddBatchReader)
            .processor(beregnRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .build()
}
