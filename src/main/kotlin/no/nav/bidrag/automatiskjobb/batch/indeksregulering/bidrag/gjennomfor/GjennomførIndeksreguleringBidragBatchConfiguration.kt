package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.gjennomfor

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
class GjennomførIndeksreguleringBidragBatchConfiguration {
    @Bean
    fun gjennomforIndeksreguleringBidragJob(
        jobRepository: JobRepository,
        gjennomforIndeksreguleringBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("gjennomforIndeksreguleringBidragJob", jobRepository)
            .listener(listener)
            .start(gjennomforIndeksreguleringBidragStep)
            .build()

    @Bean
    fun gjennomforIndeksreguleringBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        gjennomførIndeksreguleringBidragBatchReader: GjennomførIndeksreguleringBidragBatchReader,
        gjennomførIndeksreguleringBidragBatchProcessor: GjennomførIndeksreguleringBidragBatchProcessor,
        gjennomførIndeksreguleringBidragBatchWriter: GjennomførIndeksreguleringBidragBatchWriter,
    ): Step =
        StepBuilder("gjennomforIndeksreguleringBidragStep", jobRepository)
            .chunk<Indeksregulering, Indeksregulering>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(gjennomførIndeksreguleringBidragBatchReader)
            .processor(gjennomførIndeksreguleringBidragBatchProcessor)
            .writer(gjennomførIndeksreguleringBidragBatchWriter)
            .listener(gjennomførIndeksreguleringBidragBatchReader)
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(CHUNK_SIZE.toLong())
            .build()

    @Bean
    fun gjennomforIndeksreguleringBidragBatchReader(
        indeksreguleringRepository: IndeksreguleringRepository,
    ): GjennomførIndeksreguleringBidragBatchReader = GjennomførIndeksreguleringBidragBatchReader(indeksreguleringRepository, PAGE_SIZE)
}
