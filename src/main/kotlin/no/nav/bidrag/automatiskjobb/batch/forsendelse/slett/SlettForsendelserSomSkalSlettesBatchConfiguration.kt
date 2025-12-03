package no.nav.bidrag.automatiskjobb.batch.forsendelse.slett

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
class SlettForsendelserSomSkalSlettesBatchConfiguration {
    @Bean
    fun slettForsendelserSomSkalSlettesJob(
        jobRepository: JobRepository,
        slettForsendelseSomSkalSlettesBidragStep: Step,
        listener: BatchCompletionNotificationListener,
    ): Job =
        JobBuilder("slettForsendelserSomSkalSlettesJob", jobRepository)
            .listener(listener)
            .start(slettForsendelseSomSkalSlettesBidragStep)
            .build()

    @Bean
    fun slettForsendelseSomSkalSlettesBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: TaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettForsendelserSomSkalSlettesBatchReader: SlettForsendelserSomSkalSlettesBatchReader,
        slettForsendelserSomSkalSlettesProcessor: SlettForsendelserSomSkalSlettesProcessor,
        dummyItemWriter: DummyItemWriter,
    ): Step =
        StepBuilder("slettForsendelseSomSkalSlettesBidragStep", jobRepository)
            .chunk<ForsendelseBestilling, Unit>(CHUNK_SIZE, transactionManager)
            .reader(slettForsendelserSomSkalSlettesBatchReader)
            .processor(slettForsendelserSomSkalSlettesProcessor)
            .writer(dummyItemWriter)
            .taskExecutor(taskExecutor)
            .build()
}
