package no.nav.bidrag.automatiskjobb.batch.oppdaterbarn

import no.nav.bidrag.automatiskjobb.batch.BatchCompletionNotificationListener
import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.DummyItemWriter
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
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
class OppdaterBarnBatchConfiguration {
    @Bean
    fun oppdaterBarnJob(
        jobRepository: JobRepository,
        oppdaterBarnStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("oppdaterBarnJob", jobRepository)
            .listener(listener)
            .start(oppdaterBarnStep)
            .build()

    @Bean
    fun oppdaterBarnStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        oppdaterBarnBatchReader: OppdaterBarnBatchReader,
        oppdaterBarnBatchProcessor: OppdaterBarnBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("oppdaterBarnStep", jobRepository)
            .chunk<Barn, Unit>(CHUNK_SIZE, transactionManager)
            .reader(oppdaterBarnBatchReader)
            .processor(oppdaterBarnBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
