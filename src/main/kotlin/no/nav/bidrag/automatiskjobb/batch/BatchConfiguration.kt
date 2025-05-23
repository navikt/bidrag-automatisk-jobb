package no.nav.bidrag.automatiskjobb.batch

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class BatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 500
        const val PAGE_SIZE = 500
        const val GRID_SIZE = 10
    }

    @Bean
    fun batchTaskExecutor(): TaskExecutor? =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = GRID_SIZE
            maxPoolSize = 20
        }
}
