package no.nav.bidrag.automatiskjobb.batch

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor

@Configuration
class BatchConfiguration {
    companion object {
        const val CHUNK_SIZE = 500
        const val PAGE_SIZE = 500
        const val GRID_SIZE = 5
    }

    @Bean
    fun batchTaskExecutor(): TaskExecutor? =
        SimpleAsyncTaskExecutor("batch").apply {
            concurrencyLimit = GRID_SIZE
        }
}
