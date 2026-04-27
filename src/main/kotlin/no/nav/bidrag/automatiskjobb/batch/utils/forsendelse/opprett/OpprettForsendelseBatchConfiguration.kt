package no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.opprett

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
class OpprettForsendelseBatchConfiguration {
    @Bean
    fun opprettForsendelseJob(
        jobRepository: JobRepository,
        opprettForsendelseStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("opprettForsendelseJob", jobRepository)
            .listener(listener)
            .start(opprettForsendelseStep)
            .build()

    @Bean
    fun opprettForsendelseStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettForsendelseBatchReader: OpprettForsendelseBatchReader,
        opprettForsendelseBatchProcessor: OpprettForsendelseBatchProcessor,
    ): Step =
        StepBuilder("opprettForsendelseStep", jobRepository)
            .chunk<ForsendelseBestilling, Unit>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(opprettForsendelseBatchReader)
            .processor(opprettForsendelseBatchProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
