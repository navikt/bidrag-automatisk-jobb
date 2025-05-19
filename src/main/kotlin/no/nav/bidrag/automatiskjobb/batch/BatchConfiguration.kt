package no.nav.bidrag.automatiskjobb.batch

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor

@Configuration
class BatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 100
        const val PAGE_SIZE = 100
        const val GRID_SIZE = 5
        const val SKIP_LIMIT = PAGE_SIZE - 1
    }

    @Bean
    fun batchTaskExecutor(): TaskExecutor? =
        SimpleAsyncTaskExecutor("batch").apply {
            concurrencyLimit = GRID_SIZE
        }
}
