package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.brevbputland

import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.IndeksreguleringsfilService
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrevBpUtlandIndeksreguleringBidragBatchConfiguration {
    @Bean
    fun brevBpUtlandIndeksreguleringBidragJob(
        jobRepository: JobRepository,
        brevBpUtlandIndeksreguleringBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("brevBpUtlandIndeksreguleringBidragJob", jobRepository)
            .listener(listener)
            .start(brevBpUtlandIndeksreguleringBidragStep)
            .build()

    @Bean
    fun brevBpUtlandIndeksreguleringBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        brevBpUtlandIndeksreguleringBidragTasklet: BrevBpUtlandIndeksreguleringBidragTasklet,
    ): Step =
        StepBuilder("brevBpUtlandIndeksreguleringBidragStep", jobRepository)
            .tasklet(brevBpUtlandIndeksreguleringBidragTasklet, transactionManager)
            .build()

    @Bean
    fun brevBpUtlandIndeksreguleringBidragTasklet(
        indeksreguleringsfilService: IndeksreguleringsfilService,
    ): BrevBpUtlandIndeksreguleringBidragTasklet = BrevBpUtlandIndeksreguleringBidragTasklet(indeksreguleringsfilService)
}
