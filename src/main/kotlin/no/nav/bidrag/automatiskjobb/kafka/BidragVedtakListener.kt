package no.nav.bidrag.automatiskjobb.kafka

import no.nav.bidrag.automatiskjobb.SECURE_LOGGER
import no.nav.bidrag.automatiskjobb.service.OppgaveService
import no.nav.bidrag.automatiskjobb.service.VedtakService
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class BidragVedtakListener(
    private val vedtakService: VedtakService,
    private val oppgaveService: OppgaveService,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(BidragVedtakListener::class.java)
    }

//    @KafkaListener(
//        groupId = "\${VEDTAK_KAFKA_GROUP_ID_START:bidrag-automatisk-jobb-start}",
//        topics = ["\${KAFKA_VEDTAK_TOPIC}"],
//        properties = ["auto.offset.reset=earliest"],
//    )
//    fun lesHendelseFraStart(
//        hendelse: String,
//        @Header(KafkaHeaders.OFFSET) offset: Long,
//        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
//        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
//        @Header(KafkaHeaders.GROUP_ID) groupId: String,
//    ) {
//        try {
//            vedtakService.behandleVedtak(hendelse)
//        } catch (e: Exception) {
//            LOGGER.error("Det skjedde en feil ved prosessering av vedtak hendelse", e)
//        }
//    }

    @KafkaListener(
        groupId = "\${VEDTAK_KAFKA_GROUP_ID_SISTE:bidrag-automatisk-jobb-siste}",
        topics = ["\${KAFKA_VEDTAK_TOPIC}"],
        properties = ["auto.offset.reset=latest"],
    )
    fun lesHendelseFraSiste(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        LOGGER.debug("Behandler vedtakhendelse med offset: $offset i consumergroup: $groupId for topic: $topic")
        SECURE_LOGGER.debug("Behandler vedtakhendelse: $hendelse")
        try {
            val vedtakHendelse = mapVedtakHendelse(hendelse)
            oppgaveService.opprettRevurderForskuddOppgave(vedtakHendelse)
        } catch (e: Exception) {
            LOGGER.error("Det skjedde en feil ved prosessering av vedtak hendelse", e)
        }
    }

    private fun mapVedtakHendelse(hendelse: String): VedtakHendelse =
        try {
            commonObjectmapper.readValue(hendelse, VedtakHendelse::class.java)
        } finally {
            SECURE_LOGGER.debug("Leser hendelse: {}", hendelse)
        }
}
