package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.beregn.barnebidrag.utils.hentSisteLøpendePeriode
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import org.springframework.stereotype.Service

@Service
class OpprettIndeksreguleringBidragService(
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
            val løpendeStønad =
                beløpshistorikkConsumer
                    .hentLøpendeStønad(
                        HentStønadRequest(
                            stønadstype,
                            Saksnummer(barn.saksnummer),
                            Personident(barn.skyldner!!),
                            Personident(barn.kravhaver),
                        ),
                    )

            if (løpendeStønad == null) {
                secureLogger.info { "Barn: $barn for stønadstype $stønadstype har ingen løpende stønad." }
                continue
            }

            val valuta = løpendeStønad.periodeListe.hentSisteLøpendePeriode()?.valutakode
            if (valuta != null && valuta != Valutakode.NOK.name) {
                secureLogger.info {
                    "Barn: $barn for stønadstype $stønadstype har ikke NOK som valuta ($valuta) og indeksreguleres derfor ikke."
                }
                continue
            }

            val nesteIndeksreguleringsår =
                løpendeStønad.nesteIndeksreguleringsår
            if (nesteIndeksreguleringsår == null) {
                secureLogger.info {
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
}
