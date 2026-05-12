package no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.slett

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
class SlettForsendelserSomSkalSlettesBatchConfiguration {
    @Bean
    fun slettForsendelserSomSkalSlettesJob(
        jobRepository: JobRepository,
        slettForsendelseSomSkalSlettesBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("slettForsendelserSomSkalSlettesJob", jobRepository)
            .listener(listener)
            .start(slettForsendelseSomSkalSlettesBidragStep)
            .build()

    @Bean
    fun slettForsendelseSomSkalSlettesBidragStep(
        @Qualifier("batchTaskExecutor") taskExecutor: AsyncTaskExecutor,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettForsendelserSomSkalSlettesBatchReader: SlettForsendelserSomSkalSlettesBatchReader,
        slettForsendelserSomSkalSlettesProcessor: SlettForsendelserSomSkalSlettesProcessor,
    ): Step =
        StepBuilder("slettForsendelseSomSkalSlettesBidragStep", jobRepository)
            .chunk<ForsendelseBestilling, Unit>(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(slettForsendelserSomSkalSlettesBatchReader)
            .processor(slettForsendelserSomSkalSlettesProcessor)
            .writer { }
            .taskExecutor(taskExecutor)
            .build()
}
