package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.lagreb4

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
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

@Configuration
class LagreB4InformasjonBidragBatchConfiguration {
    @Bean
    fun lagreB4InformasjonBidragJob(
        jobRepository: JobRepository,
        lagreB4InformasjonBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("lagreB4InformasjonBidragJob", jobRepository)
            .listener(listener)
            .start(lagreB4InformasjonBidragStep)
            .build()

    @Bean
    fun lagreB4InformasjonBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        lagreB4InformasjonBidragBatchReader: RepositoryItemReader<Aldersjustering>,
        lagreB4InformasjonBidragBatchProcessor: LagreB4InformasjonBidragBatchProcessor,
    ): Step =
        StepBuilder("lagreB4InformasjonBidragStep", jobRepository)
            .chunk<Aldersjustering, Unit>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(lagreB4InformasjonBidragBatchReader)
            .processor(lagreB4InformasjonBidragBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()

    @Bean
    @StepScope
    fun lagreB4InformasjonBidragBatchReader(
        aldersjusteringRepository: AldersjusteringRepository,
        @Value("#{jobParameters['fattetÅr']}") fattetÅr: Long,
        @Value("#{jobParameters['barn']}") barn: String?,
    ): RepositoryItemReader<Aldersjustering> {
        val barnIds =
            barn
                ?.takeIf { it.isNotEmpty() }
                ?.split(",")
                ?.map { it.trim().toInt() }
                ?: emptyList()

        return RepositoryItemReaderBuilder<Aldersjustering>()
            .name("lagreB4InformasjonBidragBatchReader")
            .repository(aldersjusteringRepository)
            .methodName("finnFattetForÅrUtenB4Beløp")
            .arguments(listOf(fattetÅr.toInt(), barnIds))
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .saveState(false)
            .build()
    }
}
