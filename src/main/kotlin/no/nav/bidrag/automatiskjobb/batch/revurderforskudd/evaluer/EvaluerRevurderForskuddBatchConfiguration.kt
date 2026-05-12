package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager
import java.time.YearMonth

@Configuration
class EvaluerRevurderForskuddBatchConfiguration(
    @param:Value($$"${NAIS_CLUSTER_NAME:clusterName}") private val clusterName: String,
) {
    @Bean
    fun evaluerRevurderForskuddJob(
        jobRepository: JobRepository,
        evaluerRevurderForskuddStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("evaluerRevurderForskuddJob", jobRepository)
            .listener(listener)
            .start(evaluerRevurderForskuddStep)
            .build()

    @Bean
    fun evaluerRevurderForskuddStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        evaluerRevurderForskuddBatchReader: RepositoryItemReader<RevurderingForskudd>,
        evaluerRevurderForskuddBatchProcessor: EvaluerRevurderForskuddBatchProcessor,
        evaluerRevurderForskuddBatchWriter: EvaluerRevurderForskuddBatchWriter,
    ): Step =
        StepBuilder("evaluerRevurderForskuddStep", jobRepository)
            .chunk<RevurderingForskudd, RevurderingForskudd>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(evaluerRevurderForskuddBatchReader)
            .processor(evaluerRevurderForskuddBatchProcessor)
            .writer(evaluerRevurderForskuddBatchWriter)
            .taskExecutor(taskExecutor)
            .faultTolerant()
            .skipLimit(finnSkipLimit())
            .skip(Exception::class.java)
            .build()

    @Bean
    @StepScope
    fun evaluerRevurderForskuddBatchReader(
        revurderForskuddRepository: RevurderForskuddRepository,
        @Value("#{jobParameters['forManed']}") forMånedString: String?,
    ): RepositoryItemReader<RevurderingForskudd> {
        val forMåned = forMånedString?.let { YearMonth.parse(it) } ?: YearMonth.now()

        return RepositoryItemReaderBuilder<RevurderingForskudd>()
            .name("evaluerRevurderForskuddBatchReader")
            .repository(revurderForskuddRepository)
            .methodName("findAllByForMåned")
            .arguments(listOf(forMåned.toString()))
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .saveState(false)
            .build()
    }

    /**
     * Denne metoden er tiltenkt for å unngå at vi når skipLimit i dev-miljøet hvor det kan være mange tusen
     * revurderinger for en gitt måned. Det er heller ingen garanti på at grunnlag eksisterer korrekt i dev.
     *
     * I prod skal skipLimit være lik chunkSize slik at vi får en feilmelding relativt raskt og kan undersøke nærmere.
     */
    private fun finnSkipLimit(): Long {
        val skipLimit = if (clusterName == "dev-gcp") CHUNK_SIZE * 100 else CHUNK_SIZE
        return skipLimit.toLong()
    }
}
