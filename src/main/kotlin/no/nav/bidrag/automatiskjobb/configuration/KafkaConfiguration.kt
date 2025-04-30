package no.nav.bidrag.automatiskjobb.configuration

import no.nav.bidrag.automatiskjobb.SECURE_LOGGER
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
                SECURE_LOGGER.error(e) {
                    "Kafka melding med nøkkel $key, partition $partition og topic $topic feilet på offset $offset. " +
                        "Melding som feilet: $value"
                }
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
            exception,
        ) { "Håndtering av kafka melding ${record.value()} feilet. Dette er $deliveryAttempt. forsøk" }
    }

    override fun recovered(
        record: ConsumerRecord<*, *>,
        exception: java.lang.Exception,
    ) {
        SECURE_LOGGER.error(
            exception,
        ) { "Håndtering av kafka melding ${record.value()} er enten suksess eller ignorert pågrunn av ugyldig data" }
    }

    override fun recoveryFailed(
        record: ConsumerRecord<*, *>,
        original: java.lang.Exception,
        failure: java.lang.Exception,
    ) {
    }
}
