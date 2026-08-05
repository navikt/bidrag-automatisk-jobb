package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter

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
class RapporterIndeksreguleringBidragBatchConfiguration {
    @Bean
    fun rapporterIndeksreguleringBidragJob(
        jobRepository: JobRepository,
        rapporterIndeksreguleringBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("rapporterIndeksreguleringBidragJob", jobRepository)
            .listener(listener)
            .start(rapporterIndeksreguleringBidragStep)
            .build()

    @Bean
    fun rapporterIndeksreguleringBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        rapporterIndeksreguleringBidragTasklet: RapporterIndeksreguleringBidragTasklet,
    ): Step =
        StepBuilder("rapporterIndeksreguleringBidragStep", jobRepository)
            .tasklet(rapporterIndeksreguleringBidragTasklet, transactionManager)
            .build()

    @Bean
    fun rapporterIndeksreguleringBidragTasklet(rapporterService: IndeksreguleringsfilService): RapporterIndeksreguleringBidragTasklet =
        RapporterIndeksreguleringBidragTasklet(
            indeksreguleringsfilService = rapporterService,
        )
}
