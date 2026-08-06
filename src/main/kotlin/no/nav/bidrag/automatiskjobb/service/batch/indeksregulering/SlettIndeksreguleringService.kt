package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import jakarta.transaction.Transactional
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.stereotype.Service

@Service
class SlettIndeksreguleringService(
    private val indeksreguleringRepository: IndeksreguleringRepository,
) {
    @Transactional
    fun slettIndeksreguleringForÅr(år: Int) {
        secureLogger.info { "Sletter alle indeksreguleringer for år $år" }
        indeksreguleringRepository.deleteAllByÅr(år)
    }
}
