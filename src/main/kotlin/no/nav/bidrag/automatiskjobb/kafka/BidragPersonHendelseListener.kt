package no.nav.bidrag.automatiskjobb.kafka

import no.nav.bidrag.automatiskjobb.service.PersonHendelseService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class BidragPersonHendelseListener(
    private val personHendelseService: PersonHendelseService,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(PersonHendelseService::class.java)
    }

    @KafkaListener(
        topics = ["\${KAFKA_PERSON_HENDELSE_TOPIC}"],
        groupId = "\${KAFKA_GROUP_ID:bidrag-automatisk-jobb}",
    )
    fun behandlePersonHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        LOGGER.debug("Leser hendelse fra topic: $topic, offset: $offset, partition: $partition, groupId: $groupId")
        personHendelseService.behandlePersonHendelse(hendelse)
    }
}
