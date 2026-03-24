package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager
import java.time.YearMonth

@Configuration
class RevurderingslenkeRevurderForskuddBatchConfiguration {
    @Bean
    fun revurderingslenkeRevurderForskuddJob(
        jobRepository: JobRepository,
        revurderingslenkeRevurderForskuddStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("revurderingslenkeRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(revurderingslenkeRevurderForskuddStep)
            .build()

    @Bean
    fun revurderingslenkeRevurderForskuddStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        revurderingslenkeRevurderForskuddBatchReader: RepositoryItemReader<RevurderingForskudd>,
        revurderingslenkeRevurderForskuddBatchProcessor: RevurderingslenkeRevurderForskuddBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("revurderingslenkeRevurderForskuddStep", jobRepository)
            .chunk<RevurderingForskudd, Unit>(CHUNK_SIZE, transactionManager)
            .reader(revurderingslenkeRevurderForskuddBatchReader)
            .processor(revurderingslenkeRevurderForskuddBatchProcessor)
            .writer(dummyItemWriter)
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(CHUNK_SIZE)
            .build()

    @Bean
    fun revurderingslenkeRevurderForskuddBatchReader(
        revurderForskuddRepository: RevurderForskuddRepository,
        @Value("#{jobParameters['forManed']}") forMånedString: String?,
    ): RepositoryItemReader<RevurderingForskudd> {
        val forMåned = forMånedString?.let { YearMonth.parse(it) } ?: YearMonth.now()

        return RepositoryItemReaderBuilder<RevurderingForskudd>()
            .name("revurderingslenkeRevurderForskuddBatchReader")
            .repository(revurderForskuddRepository)
            .methodName("findAllByStatusIsAndForMånedIsAndVurdereTilbakekrevingIsTrueAndOppgaveIsNull")
            .arguments(listOf(Status.FATTET, forMåned.toString()))
            .saveState(false)
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .build()
    }
}
