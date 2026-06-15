package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.vedtak.request.HentVedtakForStønadRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger { }

/**
 * Tjeneste som utfører indeksregulering av bidrag.
 *
 * [indeksregulerBidrag] benyttes av opprettingsbatchen for å bygge opp indeksregulering-poster.
 * Sjekker om det allerede finnes et indeksregulerings-vedtak for inneværende år; hvis ja,
 * markeres posten med [Indeksregulering.gjennomfort] = true slik at gjennomføringsbatchen hopper
 * over den.
 *
 * [gjennomforBidrag] benyttes av gjennomføringsbatchen for å utføre selve indeksreguleringen og
 * sette [Indeksregulering.gjennomfort] til `true` etter vellykket gjennomføring.
 */
@Service
class IndeksreguleringBidragService(
    private val bidragVedtakConsumer: BidragVedtakConsumer,
) {
    fun indeksregulerBidrag(
        indeksregulering: Indeksregulering,
        barn: List<Barn>,
    ): Indeksregulering? {
        val indeksreguleringFraInneværendeÅr =
            bidragVedtakConsumer
                .hentVedtakForStønad(
                    HentVedtakForStønadRequest(
                        Saksnummer(indeksregulering.saksnummer),
                        Stønadstype.BIDRAG,
                        personidentNav,
                        Personident(indeksregulering.barn.first().kravhaver),
                    ),
                ).vedtakListe
                .filter { it.type == Vedtakstype.INDEKSREGULERING }
                .filter { it.vedtakstidspunkt.year == LocalDateTime.now().year }

        if (indeksreguleringFraInneværendeÅr.isNotEmpty()) {
            LOGGER.info {
                "Sak ${indeksregulering.saksnummer} har allerede vedtak om indeksregulering av bidrag for år ${indeksregulering.år}. " +
                    "Markerer som gjennomført."
            }
            return indeksregulering.also { it.gjennomfort = true }
        }

        LOGGER.info {
            "Oppretter indeksregulering-post for sak ${indeksregulering.saksnummer} med ${barn.size} barn."
        }
        return indeksregulering
    }

    fun gjennomforBidrag(indeksregulering: Indeksregulering): Indeksregulering {
        // TODO: Implementer selve gjennomføringen av indeksregulering av bidrag (fatte vedtak).
        LOGGER.info {
            "Gjennomføring av indeksregulering bidrag for sak ${indeksregulering.saksnummer} er ikke implementert enda."
        }
        return indeksregulering.also { it.gjennomfort = true }
    }
}
