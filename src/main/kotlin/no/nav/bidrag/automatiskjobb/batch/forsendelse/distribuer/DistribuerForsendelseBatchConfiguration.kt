package no.nav.bidrag.automatiskjobb.batch.forsendelse.distribuer

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class DistribuerForsendelseBatchConfiguration {
    @Bean
    fun distribuerForsendelseJob(
        jobRepository: JobRepository,
        distribuerForsendelseStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("distribuerForsendelseJob", jobRepository)
            .listener(listener)
            .start(distribuerForsendelseStep)
            .build()

    @Bean
    fun distribuerForsendelseStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        distribuerForsendelseBatchReader: DistribuerForsendelseBatchReader,
        distribuerForsendelseBatchProcessor: DistribuerForsendelseBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("distribuerForsendelseStep", jobRepository)
            .chunk<ForsendelseBestilling, Unit>(CHUNK_SIZE, transactionManager)
            .reader(distribuerForsendelseBatchReader)
            .processor(distribuerForsendelseBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
