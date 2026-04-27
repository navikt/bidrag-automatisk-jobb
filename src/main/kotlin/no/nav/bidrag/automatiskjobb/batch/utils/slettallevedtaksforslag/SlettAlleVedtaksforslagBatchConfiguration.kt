package no.nav.bidrag.automatiskjobb.batch.utils.slettallevedtaksforslag

import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class SlettAlleVedtaksforslagBatchConfiguration {
    @Bean
    fun slettAlleVedtaksforslagJob(
        jobRepository: JobRepository,
        slettAlleVedtaksforslagStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("slettAlleVedtaksforslagJob", jobRepository)
            .listener(listener)
            .start(slettAlleVedtaksforslagStep)
            .build()

    @Bean
    fun slettAlleVedtaksforslagStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        slettVedtaksforslagBatchReader: SlettAlleVedtaksforslagBatchReader,
        slettAlleVedtaksforslagBatchProcessor: SlettAlleVedtaksforslagBatchProcessor,
    ): Step =
        StepBuilder("slettAlleVedtaksforslagStep", jobRepository)
            .chunk<List<Int>, List<Int>>(1)
            .transactionManager(transactionManager)
            .reader(slettVedtaksforslagBatchReader)
            .processor(slettAlleVedtaksforslagBatchProcessor)
            .writer { }
            .build()
}
