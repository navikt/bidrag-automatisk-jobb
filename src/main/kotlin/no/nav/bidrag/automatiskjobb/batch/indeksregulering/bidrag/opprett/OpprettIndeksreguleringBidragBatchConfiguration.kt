package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate

@Configuration
class OpprettIndeksreguleringBidragBatchConfiguration {
    companion object {
        val BIDRAG_FREM_TIL_DATO: LocalDate = LocalDate.now().withDayOfMonth(1)
    }

    @Bean
    fun opprettIndeksreguleringBidragJob(
        jobRepository: JobRepository,
        opprettIndeksreguleringBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("opprettIndeksreguleringBidragJob", jobRepository)
            .listener(listener)
            .start(opprettIndeksreguleringBidragStep)
            .build()

    @Bean
    fun opprettIndeksreguleringBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettIndeksreguleringBidragBatchReader: OpprettIndeksreguleringBidragBatchReader,
        opprettIndeksreguleringBidragBatchProcessor: OpprettIndeksreguleringBidragBatchProcessor,
        opprettIndeksreguleringBidragBatchWriter: OpprettIndeksreguleringBidragBatchWriter,
    ): Step =
        StepBuilder("opprettIndeksreguleringBidragStep", jobRepository)
            .chunk<List<Barn>, Indeksregulering>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(opprettIndeksreguleringBidragBatchReader)
            .processor(opprettIndeksreguleringBidragBatchProcessor)
            .writer(opprettIndeksreguleringBidragBatchWriter)
            .listener(opprettIndeksreguleringBidragBatchReader)
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(CHUNK_SIZE.toLong())
            .build()

    @Bean
    fun opprettIndeksreguleringBidragBatchReader(barnRepository: BarnRepository): OpprettIndeksreguleringBidragBatchReader =
        OpprettIndeksreguleringBidragBatchReader(barnRepository, BIDRAG_FREM_TIL_DATO, CHUNK_SIZE)
}
