package no.nav.bidrag.automatiskjobb.batch.utils

import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository
import org.springframework.batch.core.configuration.support.MapJobRegistry
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.repository.support.SimpleJobRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
class BatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
        const val PAGE_SIZE = 100
        const val GRID_SIZE = 10
    }

    @Bean
    fun batchTaskExecutor(): AsyncTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = GRID_SIZE
            maxPoolSize = 30
            queueCapacity = 200
            setThreadNamePrefix("batch-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(60)
        }

    /**
     * Asynkron JobLauncher som returnerer umiddelbart etter at jobben er startet.
     * Brukes av alle batch-endepunkter slik at HTTP-requesten ikke blokkeres,
     * og slik at Spring Batch korrekt markerer jobben som STARTED → COMPLETED/FAILED
     * selv om steget kjøres parallelt via batchTaskExecutor.
     */
    @Bean
    @Primary
    fun asyncJobLauncher(
        jobRepository: JobRepository,
        batchTaskExecutor: TaskExecutor,
        jobRegistry: JobRegistry,
    ): JobOperator =
        TaskExecutorJobOperator().apply {
            setJobRepository(jobRepository)
            setTaskExecutor(batchTaskExecutor)
            setJobRegistry(jobRegistry)
            afterPropertiesSet()
        }

    @Bean
    fun jobRegistry(): JobRegistry = MapJobRegistry()
}
