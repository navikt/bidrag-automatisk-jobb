package no.nav.bidrag.automatiskjobb.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.domene.BarnetrygdBisysMelding
import no.nav.bidrag.automatiskjobb.domene.Endringsmelding
import no.nav.bidrag.automatiskjobb.domene.erIdentendring
import no.nav.bidrag.automatiskjobb.service.BaksOpphørBarnetrygdService
import no.nav.bidrag.automatiskjobb.service.OppgaveService
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class BaksOpphørBarnetrygdListener(
    private val baksOpphørBarnetrygdService: BaksOpphørBarnetrygdService,
) {
    @KafkaListener(
        topics = ["\${KAFKA_BAKS_OPPHOER_BARNETRYGD_TOPIC}"],
        groupId = "\${BAKS_OPPHOER_BARNETRYGD_KAFKA_GROUP_ID:bidrag-automatisk-jobb-v2}",
        properties = ["auto.offset.reset=earliest"],
    )
    fun behandleBarnetrygdHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        secureLogger.info { "Leser hendelse fra topic: $topic, offset: $offset, partition: $partition, groupId: $groupId" }
        try {
            val barnetrygdHendelse = commonObjectmapper.readValue(hendelse, BarnetrygdBisysMelding::class.java)
            secureLogger.info { "Behandler barnetrygdhendelse $barnetrygdHendelse" }
            baksOpphørBarnetrygdService.behandleBarnetrygdHendelse(barnetrygdHendelse)
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved behandling av barnetrygdhendelse" }
        }
    }
}
