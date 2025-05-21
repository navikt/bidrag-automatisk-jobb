package no.nav.bidrag.automatiskjobb.service

import no.nav.bidrag.automatiskjobb.domene.Endringsmelding
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Personident
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonHendelseService(
    private val barnRepository: BarnRepository,
    private val identUtils: IdentUtils,
) {
    @Transactional
    fun behandlePersonHendelse(hendelse: Endringsmelding) {
        hendelse.personidenter.forEach { personident ->
            barnRepository.findAllByKravhaver(personident).forEach { barn ->
                val nyestePersonIdent = identUtils.hentNyesteIdent(Personident(personident))
                secureLogger.info { "Behandler personhendelse og oppdaterer kravhaver ${barn.kravhaver} til $nyestePersonIdent." }
                barnRepository.save(barn.copy(kravhaver = nyestePersonIdent.verdi))
            }
            barnRepository.findAllBySkyldner(personident).forEach { barn ->
                val nyestePersonIdent = identUtils.hentNyesteIdent(Personident(personident))
                secureLogger.info { "Behandler personhendelse og oppdaterer skyldner ${barn.skyldner} til $nyestePersonIdent." }
                barnRepository.save(barn.copy(skyldner = nyestePersonIdent.verdi))
            }
        }
    }
}
