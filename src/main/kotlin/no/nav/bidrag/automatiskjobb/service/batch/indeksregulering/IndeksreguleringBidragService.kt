package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger { }

@Service
class IndeksreguleringBidragService(
    private val beløpshistorikkConsumer: BidragBeløpshistorikkConsumer,
) {
    fun opprettIndeksregulering(
        batchId: String,
        år: Int,
        barn: Barn,
        stønadstyper: List<Stønadstype>,
    ): List<Indeksregulering>? {
        val indeksreguleringer = mutableListOf<Indeksregulering>()

        for (stønadstype in stønadstyper) {
            val nesteIndeksreguleringsår =
                beløpshistorikkConsumer
                    .hentLøpendeStønad(
                        HentStønadRequest(
                            stønadstype,
                            Saksnummer(barn.saksnummer),
                            Personident(barn.skyldner!!),
                            Personident(barn.kravhaver),
                        ),
                    )?.nesteIndeksreguleringsår

            if (nesteIndeksreguleringsår == null) {
                secureLogger.debug {
                    "Barn: $barn for stønadstype $stønadstype mangler indeksreguleringsår og indeksreguleres derfor ikke."
                }
                continue
            }

            if (nesteIndeksreguleringsår > år) {
                secureLogger.info {
                    "Barn: $barn for stønadstype $stønadstype har indeksreguleringsår frem i tid og indeksreguleres derfor ikke."
                }
                continue
            }

            secureLogger.info {
                "Oppretter indeksregulering for $barn for stønadstype $stønadstype."
            }
            indeksreguleringer.add(
                Indeksregulering(
                    batchId = batchId,
                    år = år,
                    barn = barn,
                    stønadstype = stønadstype,
                    status = Status.UBEHANDLET,
                ),
            )
        }
        return indeksreguleringer
    }

    fun gjennomførIndeksregulering(indeksregulering: Indeksregulering): Indeksregulering {
        // TODO: Implementer selve gjennomføringen av indeksregulering av bidrag (fatte vedtak).
        // VIKTIG!! IKKE INDEKSREGULER NOE SOM LØPER I ANNET ENN NOK
        LOGGER.info {
            "Gjennomføring av indeksregulering bidrag for sak ${indeksregulering.barn.saksnummer} er ikke implementert enda."
        }
        return indeksregulering.also { it.gjennomfort = true }
    }
}
