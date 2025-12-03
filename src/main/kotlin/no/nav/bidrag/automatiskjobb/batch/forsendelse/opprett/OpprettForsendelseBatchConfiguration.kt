package no.nav.bidrag.automatiskjobb.batch.forsendelse.opprett

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
class OpprettForsendelseBatchConfiguration {
    @Bean
    fun opprettForsendelseJob(
        jobRepository: JobRepository,
        opprettForsendelseStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("opprettForsendelseJob", jobRepository)
            .listener(listener)
            .start(opprettForsendelseStep)
            .build()

    @Bean
    fun opprettForsendelseStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        opprettForsendelseBatchReader: OpprettForsendelseBatchReader,
        opprettForsendelseBatchProcessor: OpprettForsendelseBatchProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("opprettForsendelseStep", jobRepository)
            .chunk<ForsendelseBestilling, Unit>(CHUNK_SIZE, transactionManager)
            .reader(opprettForsendelseBatchReader)
            .processor(opprettForsendelseBatchProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
