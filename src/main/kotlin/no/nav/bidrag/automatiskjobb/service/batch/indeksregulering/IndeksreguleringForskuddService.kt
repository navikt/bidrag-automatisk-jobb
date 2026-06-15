package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger { }

/**
 * Tjeneste som utfører indeksregulering av forskudd.
 *
 * Inneholder foreløpig ingen logikk – selve indeksreguleringen skal implementeres senere.
 * Per nå benyttes [indeksregulerForskudd] kun for å bygge opp indeksregulering-poster som lagres
 * med en status, slik at batchen kan kjøres flere ganger uten å utføre indeksreguleringen på nytt.
 */
@Service
class IndeksreguleringForskuddService {
    fun indeksregulerForskudd(
        indeksregulering: Indeksregulering,
        barn: List<Barn>,
    ): Indeksregulering? {
        // TODO: Implementer selve indeksreguleringen av forskudd.
        LOGGER.info {
            "Indeksregulering forskudd for sak ${indeksregulering.saksnummer} er ikke implementert enda. " +
                "Oppretter kun indeksregulering-post for ${barn.size} barn."
        }
        return indeksregulering
    }
}
