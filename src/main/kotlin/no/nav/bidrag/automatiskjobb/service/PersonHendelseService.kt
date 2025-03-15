package no.nav.bidrag.automatiskjobb.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.automatiskjobb.SECURE_LOGGER
import no.nav.bidrag.automatiskjobb.domene.Endringsmelding
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonHendelseService(
    private val barnRepository: BarnRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun behandlePersonHendelse(hendelse: String) {
        val personHendelse = objectMapper.readValue(hendelse, Endringsmelding::class.java)
        personHendelse.personidenter.forEach { personident ->

            barnRepository.findAllByKravhaver(personident).forEach { barn ->
                SECURE_LOGGER.info("Behandler personhendelse og oppdaterer kravhaver ${barn.kravhaver} til $personident.")
                barnRepository.save(barn.copy(kravhaver = personident))
            }
            barnRepository.findAllBySkyldner(personident).forEach { barn ->
                SECURE_LOGGER.info("Behandler personhendelse og oppdaterer skyldner ${barn.skyldner} til $personident.")
                barnRepository.save(barn.copy(skyldner = personident))
            }
        }
    }
}
