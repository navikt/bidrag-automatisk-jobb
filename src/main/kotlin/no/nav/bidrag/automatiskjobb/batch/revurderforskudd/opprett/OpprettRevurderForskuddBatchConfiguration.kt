package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.data.domain.Sort
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
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettRevurderForskuddBatchReader: RepositoryItemReader<Barn>,
        opprettRevurderForskuddBatchProcessor: OpprettRevurderForskuddBatchProcessor,
        dummmyWriter: DummyItemWriter,
    ): Step =
        StepBuilder("opprettRevurderForskuddStep", jobRepository)
            .chunk<Barn, RevurderingForskudd>(CHUNK_SIZE, transactionManager)
            .reader(opprettRevurderForskuddBatchReader)
            .processor(opprettRevurderForskuddBatchProcessor)
            .writer(dummmyWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun opprettRevurderForskuddBatchReader(barnRepository: BarnRepository): RepositoryItemReader<Barn> =
        RepositoryItemReaderBuilder<Barn>()
            .name("opprettRevurderForskuddBatchReader")
            .repository(barnRepository)
            .methodName("findBarnSomSkalRevurdereForskudd")
            .arguments(listOf(FORSKUDD_FREM_TIL_DATO))
            .saveState(false) // Savestate må være false for å unngå feil ved parallell kjøring
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .build()
}
