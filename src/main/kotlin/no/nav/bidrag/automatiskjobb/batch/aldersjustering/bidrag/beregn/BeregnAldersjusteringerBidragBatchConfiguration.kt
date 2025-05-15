package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import jakarta.persistence.EntityManagerFactory
import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.StatusJpaPagingItemReader
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultat
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
class BeregnAldersjusteringerBidragBatchConfiguration {
    @Bean
    fun beregnAldersjusteringerBidragJob(
        jobRepository: JobRepository,
        beregnAldersjusteringerBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("beregnAldersjusteringerBidragJob", jobRepository)
            .listener(listener)
            .start(beregnAldersjusteringerBidragStep)
            .build()

    @Bean
    fun beregnAldersjusteringerBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        beregnAldersjusteringJpaPagingItemReader: JpaPagingItemReader<Aldersjustering>,
        beregnAldersjusteringerBidragBatchProcessor: BeregnAldersjusteringerBidragBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("beregnAldersjusteringerBidragStep", jobRepository)
            .chunk<Aldersjustering, AldersjusteringResultat>(CHUNK_SIZE, transactionManager)
            .reader(beregnAldersjusteringJpaPagingItemReader)
            .processor(beregnAldersjusteringerBidragBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun beregnAldersjusteringJpaPagingItemReader(entityManagerFactory: EntityManagerFactory): JpaPagingItemReader<Aldersjustering> {
        val queryProvider =
            JpaNativeQueryProvider<Aldersjustering>().apply {
                setSqlQuery("SELECT a FROM Aldersjustering a WHERE a.status IN (:statuses) ORDER BY a.id ASC")
                setEntityClass(Aldersjustering::class.java)
            }
        val reader =
            StatusJpaPagingItemReader<Aldersjustering>().apply {
                setQueryProvider(queryProvider)
                setParameterValues(
                    mapOf(
                        "statuses" to listOf(Status.UBEHANDLET.name, Status.FEILET.name, Status.SLETTET.name, Status.SIMULERT.name),
                    ),
                )
                setEntityManagerFactory(entityManagerFactory)
                pageSize = PAGE_SIZE
                isSaveState = false
            }
        return reader
    }
}
