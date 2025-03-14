package no.nav.bidrag.automatiskjobb.configuration

import no.nav.bidrag.automatiskjobb.SECURE_LOGGER
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.RetryListener
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
class KafkaConfiguration {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(KafkaConfiguration::class.java)
    }

    @Bean
    fun consumerFactoryPersonHendelse(
        @Value("\${KAFKA_GROUP_ID:bidrag-automatisk-jobb}") groupId: String,
        @Value("\${KAFKA_BROKERS:http://localhost:9092}") brokers: String,
    ): ConsumerFactory<String, String> {
        val props =
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers,
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
            )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactoryPersonHendelse(
        consumerFactoryPersonHendelse: ConsumerFactory<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactoryPersonHendelse
        return factory
    }

    @Bean
    fun defaultErrorHandler(
        @Value("\${KAFKA_MAX_RETRY:-1}") maxRetry: Int,
    ): DefaultErrorHandler {
        // Max retry should not be set in production
        val backoffPolicy = if (maxRetry == -1) ExponentialBackOff() else ExponentialBackOffWithMaxRetries(maxRetry)
        backoffPolicy.multiplier = 1.2
        backoffPolicy.maxInterval = 300000L // 5 mins
        LOGGER.info(
            "Initializing Kafka errorhandler with backoffpolicy {}, maxRetry={}",
            backoffPolicy,
            maxRetry,
        )
        val errorHandler =
            DefaultErrorHandler({ rec, e ->
                val key = rec.key()
                val value = rec.value()
                val offset = rec.offset()
                val topic = rec.topic()
                val partition = rec.partition()
                SECURE_LOGGER.error(
                    "Kafka melding med nøkkel $key, partition $partition og topic $topic feilet på offset $offset. " +
                        "Melding som feilet: $value",
                    e,
                )
            }, backoffPolicy)
        errorHandler.setRetryListeners(KafkaRetryListener())
        return errorHandler
    }
}

class KafkaRetryListener : RetryListener {
    override fun failedDelivery(
        record: ConsumerRecord<*, *>,
        exception: Exception,
        deliveryAttempt: Int,
    ) {
        SECURE_LOGGER.error(
            "Håndtering av kafka melding ${record.value()} feilet. Dette er $deliveryAttempt. forsøk",
            exception,
        )
    }

    override fun recovered(
        record: ConsumerRecord<*, *>,
        exception: java.lang.Exception,
    ) {
        SECURE_LOGGER.error(
            "Håndtering av kafka melding ${record.value()} er enten suksess eller ignorert pågrunn av ugyldig data",
            exception,
        )
    }

    override fun recoveryFailed(
        record: ConsumerRecord<*, *>,
        original: java.lang.Exception,
        failure: java.lang.Exception,
    ) {
    }
}
