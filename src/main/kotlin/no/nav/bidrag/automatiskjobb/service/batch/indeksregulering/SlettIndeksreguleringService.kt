package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.stereotype.Service

@Service
class SlettIndeksreguleringService(
    private val indeksreguleringRepository: IndeksreguleringRepository,
) {
    fun slettIndeksreguleringForÅr(år: Int) {
        secureLogger.info { "Sletter alle indeksreguleringer for år $år" }
        indeksreguleringRepository.deleteAllByÅr(år)
    }
}
