package no.nav.bidrag.automatiskjobb.configuration

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.RetryListener
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.util.backoff.ExponentialBackOff

private val LOGGER = KotlinLogging.logger { }

@Configuration
class KafkaConfiguration {
    @Bean
    fun defaultErrorHandler(
        @Value($$"${KAFKA_MAX_RETRY:-1}") maxRetry: Int,
    ): DefaultErrorHandler {
        // Max retry should not be set in production
        val backoffPolicy = if (maxRetry == -1) ExponentialBackOff() else ExponentialBackOffWithMaxRetries(maxRetry)
        backoffPolicy.multiplier = 1.2
        backoffPolicy.maxInterval = 300000L // 5 mins
        LOGGER.info { "${"Initializing Kafka errorhandler with backoffpolicy {}, maxRetry={}"} $backoffPolicy $maxRetry" }
        val errorHandler =
            DefaultErrorHandler({ rec, e ->
                val key = rec.key()
                val value = rec.value()
                val offset = rec.offset()
                val topic = rec.topic()
                val partition = rec.partition()
                LOGGER.error(e) {
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
        LOGGER.error(
            exception,
        ) {
            "Håndtering av kafka melding i topic ${record.topic()} med offset ${record.offset()} nøkkel ${record.key()} og innhold ${record.value()} feilet. Dette er $deliveryAttempt. forsøk"
        }
    }

    override fun recovered(
        record: ConsumerRecord<*, *>,
        exception: java.lang.Exception,
    ) {
        LOGGER.error(
            exception,
        ) {
            "Håndtering av kafka melding i topic ${record.topic()} med offset ${record.offset()} nøkkel ${record.key()} og innhold ${record.value()} er enten suksess eller ignorert pågrunn av ugyldig data"
        }
    }

    override fun recoveryFailed(
        record: ConsumerRecord<*, *>,
        original: java.lang.Exception,
        failure: java.lang.Exception,
    ) {
    }
}
