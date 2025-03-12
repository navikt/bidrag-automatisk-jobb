package no.nav.bidrag.aldersjustering.batch

import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AldersjusteringBatchConfiguration {
    @Bean
    fun beanRemover(): BeanDefinitionRegistryPostProcessor =
        BeanDefinitionRegistryPostProcessor { registry: BeanDefinitionRegistry ->
            registry.removeBeanDefinition("jobRegistryBeanPostProcessor")
            registry.removeBeanDefinition("annotationActionEndpointMapping")
        }
}
