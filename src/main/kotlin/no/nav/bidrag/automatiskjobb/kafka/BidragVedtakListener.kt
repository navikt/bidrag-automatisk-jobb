package no.nav.bidrag.automatiskjobb.kafka

import no.nav.bidrag.automatiskjobb.SECURE_LOGGER
import no.nav.bidrag.automatiskjobb.service.VedtakService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class BidragVedtakListener(
    private val vedtakService: VedtakService,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(BidragVedtakListener::class.java)
    }

    @KafkaListener(
        groupId = "\${KAFKA_GROUP_ID:bidrag-automatisk-jobb}",
        topics = ["\${KAFKA_VEDTAK_TOPIC}"],
    )
    fun lesHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        LOGGER.info("Behandler vedtakhendelse med offset: $offset i consumergroup: $groupId for topic: $topic")
        SECURE_LOGGER.info("Behandler vedtakhendelse: $hendelse")
        vedtakService.behandleVedtak(hendelse)
    }
}
