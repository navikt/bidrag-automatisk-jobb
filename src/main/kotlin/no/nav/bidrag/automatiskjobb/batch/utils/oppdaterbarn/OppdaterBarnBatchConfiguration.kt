package no.nav.bidrag.automatiskjobb.batch.utils.oppdaterbarn

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
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
class OppdaterBarnBatchConfiguration {
    @Bean
    fun oppdaterBarnJob(
        jobRepository: JobRepository,
        oppdaterBarnStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("oppdaterBarnJob", jobRepository)
            .listener(listener)
            .start(oppdaterBarnStep)
            .build()

    @Bean
    fun oppdaterBarnStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        oppdaterBarnBatchReader: OppdaterBarnBatchReader,
        oppdaterBarnBatchProcessor: OppdaterBarnBatchProcessor,
    ): Step =
        StepBuilder("oppdaterBarnStep", jobRepository)
            .chunk<Barn, Unit>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(oppdaterBarnBatchReader)
            .processor(oppdaterBarnBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
