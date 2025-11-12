package no.nav.bidrag.automatiskjobb.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.service.OppgaveService
import no.nav.bidrag.automatiskjobb.service.VedtakService
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class BidragVedtakListener(
    private val vedtakService: VedtakService,
    private val oppgaveService: OppgaveService,
) : ConsumerSeekAware {
    @KafkaListener(
        groupId = "\${VEDTAK_KAFKA_GROUP_ID_START:bidrag-automatisk-jobb-start}",
        topics = ["\${KAFKA_VEDTAK_TOPIC}"],
        properties = ["auto.offset.reset=earliest"],
    )
    fun lesHendelseFraStart(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        behandleHendelse(offset, groupId, topic, hendelse, vedtakService::behandleVedtak)
    }

    @KafkaListener(
        groupId = "\${VEDTAK_KAFKA_GROUP_ID_SISTE:bidrag-automatisk-jobb-siste}",
        topics = ["\${KAFKA_VEDTAK_TOPIC}"],
        properties = ["auto.offset.reset=latest"],
    )
    fun lesHendelseFraSiste(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        behandleHendelse(offset, groupId, topic, hendelse, oppgaveService::opprettRevurderForskuddOppgave)
    }

    private fun behandleHendelse(
        offset: Long,
        groupId: String,
        topic: String,
        hendelse: String,
        metode: (vedtakHendelse: VedtakHendelse) -> Unit,
    ) {
        handleInternal(offset, groupId, topic, hendelse) { metode(it) }
    }

    private fun handleInternal(
        offset: Long,
        groupId: String,
        topic: String,
        hendelse: String,
        handler: (VedtakHendelse) -> Unit,
    ) {
        LOGGER.info { "Behandler vedtakhendelse $hendelse med offset: $offset i consumergroup: $groupId for topic: $topic" }
        try {
            val vedtakHendelse = mapVedtakHendelse(hendelse)
            handler(vedtakHendelse)
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved prosessering av vedtak hendelse" }
            throw e
        }
    }

    private fun mapVedtakHendelse(hendelse: String): VedtakHendelse =
        try {
            commonObjectmapper.readValue(hendelse, VedtakHendelse::class.java)
        } finally {
            LOGGER.debug { "${"Leser hendelse: {}"} $hendelse" }
        }
}
