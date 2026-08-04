package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.fattvedtak

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class FattVedtakIndeksreguleringBidragBatchConfiguration {
    @Bean
    fun fattVedtakIndeksreguleringBidragJob(
        jobRepository: JobRepository,
        fattVedtakIndeksreguleringBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("fattVedtakIndeksreguleringBidragJob", jobRepository)
            .listener(listener)
            .start(fattVedtakIndeksreguleringBidragStep)
            .build()

    @Bean
    fun fattVedtakIndeksreguleringBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        fattVedtakIndeksreguleringBidragBatchReader: FattVedtakIndeksreguleringBidragBatchReader,
        fattVedtakIndeksreguleringBidragBatchProcessor: FattVedtakIndeksreguleringBidragBatchProcessor,
    ): Step =
        StepBuilder("fattVedtakIndeksreguleringBidragStep", jobRepository)
            .chunk<Indeksregulering, Unit>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(fattVedtakIndeksreguleringBidragBatchReader)
            .processor(fattVedtakIndeksreguleringBidragBatchProcessor)
            .writer { }
            .listener(fattVedtakIndeksreguleringBidragBatchReader)
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(CHUNK_SIZE.toLong())
            .build()

    @Bean
    fun fattVedtakIndeksreguleringBidragBatchReader(
        indeksreguleringRepository: IndeksreguleringRepository,
    ): FattVedtakIndeksreguleringBidragBatchReader = FattVedtakIndeksreguleringBidragBatchReader(indeksreguleringRepository, PAGE_SIZE)
}
