package no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.distribuer

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class DistribuerForsendelseBatchConfiguration {
    @Bean
    fun distribuerForsendelseJob(
        jobRepository: JobRepository,
        distribuerForsendelseStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("distribuerForsendelseJob", jobRepository)
            .listener(listener)
            .start(distribuerForsendelseStep)
            .build()

    @Bean
    fun distribuerForsendelseStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        distribuerForsendelseBatchReader: DistribuerForsendelseBatchReader,
        distribuerForsendelseBatchProcessor: DistribuerForsendelseBatchProcessor,
    ): Step =
        StepBuilder("distribuerForsendelseStep", jobRepository)
            .chunk<ForsendelseBestilling, Unit>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(distribuerForsendelseBatchReader)
            .processor(distribuerForsendelseBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
