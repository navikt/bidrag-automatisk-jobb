package no.nav.bidrag.automatiskjobb.batch

import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.SimpleJobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@EnableBatchProcessing
class AldersjusteringBatchConfiguration {
    @Bean
    fun beanRemover(): BeanDefinitionRegistryPostProcessor =
        BeanDefinitionRegistryPostProcessor { registry: BeanDefinitionRegistry ->
            registry.removeBeanDefinition("annotationActionEndpointMapping")
        }

    @Bean
    @Primary
    fun jobOperator(
        jobLauncher: JobLauncher,
        jobRepository: JobRepository,
        jobRegistry: JobRegistry,
        jobExplorer: JobExplorer,
    ): SimpleJobOperator {
        val jobOperator = SimpleJobOperator()
        jobOperator.setJobLauncher(jobLauncher)
        jobOperator.setJobRepository(jobRepository)
        jobOperator.setJobRegistry(jobRegistry)
        jobOperator.setJobExplorer(jobExplorer)
        return jobOperator
    }
}
