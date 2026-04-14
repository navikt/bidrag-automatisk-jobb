package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate

@Configuration
class OpprettRevurderForskuddBatchConfiguration {
    companion object {
        val FORSKUDD_FREM_TIL_DATO: LocalDate = LocalDate.now().withDayOfMonth(1)
    }

    @Bean
    fun opprettRevurderForskuddJob(
        jobRepository: JobRepository,
        opprettRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("opprettRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(opprettRevurderForskuddStep)
            .build()

    @Bean
    fun opprettRevurderForskuddStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettRevurderForskuddBatchReader: OpprettRevurderForskuddBatchReader,
        opprettRevurderForskuddBatchProcessor: OpprettRevurderForskuddBatchProcessor,
        opprettRevurderForskuddBatchWriter: OpprettRevurderForskuddBatchWriter,
    ): Step =
        StepBuilder("opprettRevurderForskuddStep", jobRepository)
            .chunk<List<Barn>, RevurderingForskudd>(CHUNK_SIZE, transactionManager)
            .reader(opprettRevurderForskuddBatchReader)
            .processor(opprettRevurderForskuddBatchProcessor)
            .writer(opprettRevurderForskuddBatchWriter)
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(CHUNK_SIZE)
            .build()

    @Bean
    fun opprettRevurderForskuddBatchReader(barnRepository: BarnRepository): OpprettRevurderForskuddBatchReader =
        OpprettRevurderForskuddBatchReader(barnRepository, FORSKUDD_FREM_TIL_DATO, CHUNK_SIZE)
}
