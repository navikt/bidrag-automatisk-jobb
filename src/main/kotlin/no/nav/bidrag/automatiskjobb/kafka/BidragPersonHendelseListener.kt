package no.nav.bidrag.automatiskjobb.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.domene.Endringsmelding
import no.nav.bidrag.automatiskjobb.service.PersonHendelseService
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class BidragPersonHendelseListener(
    private val personHendelseService: PersonHendelseService,
    private val revurderForskuddService: RevurderForskuddService,
) {
    @KafkaListener(
        topics = ["\${KAFKA_PERSON_HENDELSE_TOPIC}"],
        groupId = "\${KAFKA_GROUP_ID:bidrag-automatisk-jobb2}",
    )
    fun behandlePersonHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        LOGGER.debug { "Leser hendelse fra topic: $topic, offset: $offset, partition: $partition, groupId: $groupId" }
        try {
            val personHendelse = commonObjectmapper.readValue(hendelse, Endringsmelding::class.java)
            secureLogger.info { "Behandler person hendelse $personHendelse" }
            personHendelseService.behandlePersonHendelse(personHendelse)

            if (personHendelse.adresseendring != null) {
                try {
                    secureLogger.info {
                        "Sjekker for person om barn mottar forskudd og fortsatt bor hos BM etter adresseendring i hendelse $personHendelse"
                    }
                    revurderForskuddService.skalBMFortsattMottaForskuddForSøknadsbarn(personHendelse.aktørid)
                } catch (e: Exception) {
                    LOGGER.error(e) { "Det skjedde en feil ved sjekk om BM fortsatt skal motta forskudd for barn" }
                }
            }
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved behandling av personhendelse" }
        }
    }
}
