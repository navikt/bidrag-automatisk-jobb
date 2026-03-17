package no.nav.bidrag.automatiskjobb.batch

import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class BatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
        const val PAGE_SIZE = 100
        const val GRID_SIZE = 10
    }

    @Bean
    fun batchTaskExecutor(): TaskExecutor =
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
    ): JobLauncher =
        TaskExecutorJobLauncher().apply {
            setJobRepository(jobRepository)
            setTaskExecutor(batchTaskExecutor)
            afterPropertiesSet()
        }
}
