package no.nav.bidrag.automatiskjobb.service

import no.nav.bidrag.automatiskjobb.SECURE_LOGGER
import no.nav.bidrag.automatiskjobb.domene.Endringsmelding
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonHendelseService(
    private val barnRepository: BarnRepository,
) {
    @Transactional
    fun behandlePersonHendelse(hendelse: Endringsmelding) {
        hendelse.personidenter.forEach { personident ->

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
