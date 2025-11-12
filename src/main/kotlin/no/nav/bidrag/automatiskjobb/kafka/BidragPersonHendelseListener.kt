package no.nav.bidrag.automatiskjobb.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.domene.Endringsmelding
import no.nav.bidrag.automatiskjobb.domene.erIdentendring
import no.nav.bidrag.automatiskjobb.service.OppgaveService
import no.nav.bidrag.automatiskjobb.service.PersonHendelseService
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class BidragPersonHendelseListener(
    private val personHendelseService: PersonHendelseService,
    private val oppgaveService: OppgaveService,
) {
    @KafkaListener(
        topics = ["\${KAFKA_PERSON_HENDELSE_TOPIC}"],
        groupId = "\${PERSON_HENDELSE_KAFKA_GROUP_ID:bidrag-automatisk-jobb}",
        properties = ["auto.offset.reset=latest"],
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
            LOGGER.info { "Behandler person hendelse $personHendelse" }
            if (personHendelse.erIdentendring) {
                personHendelseService.behandlePersonHendelse(personHendelse)
            }

            oppgaveService.sjekkOgOpprettRevurderForskuddOppgaveEtterBarnFlyttetFraBM(personHendelse)
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved behandling av personhendelse" }
        }
    }
}
