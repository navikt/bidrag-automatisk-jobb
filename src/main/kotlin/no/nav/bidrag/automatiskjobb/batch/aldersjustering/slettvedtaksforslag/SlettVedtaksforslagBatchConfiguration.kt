package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
class SlettVedtaksforslagBatchConfiguration {
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
    fun slettVedtaksforslagStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettVedtaksforslagReader: ItemReader<Aldersjustering>,
        slettVedtaksforslagBatchProcessor: SlettVedtaksforslagBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("slettVedtaksforslagStep", jobRepository)
            .chunk<Aldersjustering, Aldersjustering>(CHUNK_SIZE, transactionManager)
            .reader(slettVedtaksforslagReader)
            .processor(slettVedtaksforslagBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    fun slettVedtaksforslagReader(dataSource: DataSource): ItemReader<Aldersjustering> {
        val reader = JdbcPagingItemReader<Aldersjustering>()
        val sqlPagingQuaryPoviderFactoryBean =
            SqlPagingQueryProviderFactoryBean().apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM aldersjustering")
                setWhereClause("WHERE status IS 'SLETTES'")
                setSortKey("id")
            }
        try {
            reader.setQueryProvider(sqlPagingQuaryPoviderFactoryBean.`object`)
            reader.pageSize = 100
            reader.setRowMapper(BeanPropertyRowMapper(Aldersjustering::class.java))
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
        return reader
    }
}
