package no.nav.bidrag.automatiskjobb.batch.indeksregulering.forskudd.opprett

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
class OpprettIndeksreguleringForskuddBatchConfiguration {
    companion object {
        val FORSKUDD_FREM_TIL_DATO: LocalDate = LocalDate.now().withDayOfMonth(1)
    }

    @Bean
    fun opprettIndeksreguleringForskuddJob(
        jobRepository: JobRepository,
        opprettIndeksreguleringForskuddStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("opprettIndeksreguleringForskuddJob", jobRepository)
            .listener(listener)
            .start(opprettIndeksreguleringForskuddStep)
            .build()

    @Bean
    fun opprettIndeksreguleringForskuddStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettIndeksreguleringForskuddBatchReader: OpprettIndeksreguleringForskuddBatchReader,
        opprettIndeksreguleringForskuddBatchProcessor: OpprettIndeksreguleringForskuddBatchProcessor,
        opprettIndeksreguleringForskuddBatchWriter: OpprettIndeksreguleringForskuddBatchWriter,
    ): Step =
        StepBuilder("opprettIndeksreguleringForskuddStep", jobRepository)
            .chunk<List<Barn>, Indeksregulering>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(opprettIndeksreguleringForskuddBatchReader)
            .processor(opprettIndeksreguleringForskuddBatchProcessor)
            .writer(opprettIndeksreguleringForskuddBatchWriter)
            .listener(opprettIndeksreguleringForskuddBatchReader)
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(CHUNK_SIZE.toLong())
            .build()

    @Bean
    fun opprettIndeksreguleringForskuddBatchReader(barnRepository: BarnRepository): OpprettIndeksreguleringForskuddBatchReader =
        OpprettIndeksreguleringForskuddBatchReader(barnRepository, FORSKUDD_FREM_TIL_DATO, CHUNK_SIZE)
}
