package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag

import jakarta.persistence.EntityManagerFactory
import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.StatusJpaPagingItemReader
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JpaPagingItemReader
import org.springframework.batch.item.database.orm.JpaNativeQueryProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class SlettVedtaksforslagBatchConfiguration {
    @Bean
    fun slettVedtaksforslagStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettVedtaksforslagBatchReader: SlettVedtaksforslagBatchReader,
        slettVedtaksforslagBatchProcessor: SlettVedtaksforslagBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("slettVedtaksforslagStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(slettVedtaksforslagBatchReader)
            .processor(slettVedtaksforslagBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun slettVedtaksforslagJob(
        jobRepository: JobRepository,
        slettVedtaksforslagStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("slettVedtaksforslagJob", jobRepository)
            .listener(listener)
            .start(slettVedtaksforslagStep)
            .build()

    @Bean
    fun slettVedtaksforslagJpaPagingItemReader(entityManagerFactory: EntityManagerFactory): JpaPagingItemReader<Aldersjustering> {
        val queryProvider =
            JpaNativeQueryProvider<Aldersjustering>().apply {
                setSqlQuery("SELECT a FROM Aldersjustering a WHERE a.status = :status")
                setEntityClass(Aldersjustering::class.java)
            }
        val reader =
            StatusJpaPagingItemReader<Aldersjustering>().apply {
                setQueryProvider(queryProvider)
                setParameterValues(mapOf("status" to listOf(Status.SLETTES.name)))
                setEntityManagerFactory(entityManagerFactory)
                pageSize = PAGE_SIZE
                isSaveState = false
            }
        return reader
    }
}
