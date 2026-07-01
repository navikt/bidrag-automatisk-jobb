package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

@Service
class BarnService(
    private val bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer,
    private val barnRepository: BarnRepository,
) {
    fun oppdaterBarnForskuddOgBidragPerioder(
        barn: Barn,
        simuler: Boolean,
    ) {
        oppdaterForskudd(barn)
        oppdaterBidrag(barn)
        oppdater18ÅrsBidrag(barn)
        oppdaterOppforstringsbidrag(barn)
        if (simuler) {
            LOGGER.info {
                "Simuleringmodus er på. Gjør ingen endring på periodene for forskudd/bidrag for barn ${barn.infoUtenPerioder()}"
            }
            return
        }
        barn.oppdatert = LocalDateTime.now()
        barnRepository.save(barn)
    }

    private fun oppdaterOppforstringsbidrag(barn: Barn) {
        val oppforstringsbidrag =
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                barn.tilHentStønadHistoriskRequest(
                    Stønadstype.OPPFOSTRINGSBIDRAG,
                ),
            ) ?: run {
                LOGGER.info { "Fant ingen oppforstringsbidrag stønader for barn ${barn.infoMedPerioder()}" }
                return
            }

        if (oppforstringsbidrag.periodeListe.isEmpty()) {
            LOGGER.info {
                "Ingen oppforstringsbidrag perioder funnet for barn ${barn.infoMedPerioder()}"
            }
            return
        }

        if (oppforstringsbidrag.periodeFom() != barn.oppfostringsbidragFra ||
            oppforstringsbidrag.periodeTil() != barn.oppfostringsbidragTil
        ) {
            LOGGER.info {
                "Feil oppforstringsbidrag periode lagret for barn ${barn.infoUtenPerioder()}. Oppdaterer " +
                    "fra ${barn.oppfostringsbidragFra} - ${barn.oppfostringsbidragTil} til ${oppforstringsbidrag.periodeFom()} - ${oppforstringsbidrag.periodeTil()}"
            }
        }
        barn.oppfostringsbidragFra = oppforstringsbidrag.periodeFom()
        barn.oppfostringsbidragTil = oppforstringsbidrag.periodeTil()
    }

    private fun oppdater18ÅrsBidrag(barn: Barn) {
        val bidrag18År =
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                barn.tilHentStønadHistoriskRequest(
                    Stønadstype.BIDRAG18AAR,
                ),
            ) ?: run {
                LOGGER.info { "Fant ingen 18 års bidrag stønader for barn ${barn.infoMedPerioder()}" }
                return
            }

        if (bidrag18År.periodeListe.isEmpty()) {
            LOGGER.info {
                "Ingen 18 års bidrag perioder funnet for barn ${barn.infoMedPerioder()}"
            }
            return
        }

        if (bidrag18År.periodeFom() != barn.bidrag18ÅrFra || bidrag18År.periodeTil() != barn.bidrag18ÅrTil) {
            LOGGER.info {
                "Feil 18 års bidrag periode lagret for barn ${barn.infoUtenPerioder()}. Oppdaterer " +
                    "fra ${barn.bidrag18ÅrFra} - ${barn.bidrag18ÅrTil} til ${bidrag18År.periodeFom()} - ${bidrag18År.periodeTil()}"
            }
        }
        barn.bidrag18ÅrFra = bidrag18År.periodeFom()
        barn.bidrag18ÅrTil = bidrag18År.periodeTil()
    }

    private fun oppdaterForskudd(barn: Barn) {
        val forskuddStønad =
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                barn.tilHentStønadHistoriskRequest(Stønadstype.FORSKUDD),
            ) ?: run {
                LOGGER.info { "Fant ingen forskudd stønader for barn ${barn.infoMedPerioder()}" }
                return
            }

        if (forskuddStønad.periodeListe.isEmpty() && barn.forskuddFra == null && barn.forskuddTil == null) {
            LOGGER.info { "Ingen forskudd perioder funnet for barn ${barn.infoMedPerioder()}" }
            return
        }

        if (forskuddStønad.periodeListe.isEmpty()) {
            LOGGER.info {
                "Ingen forskudd perioder funnet for barn ${barn.infoMedPerioder()} " +
                    "men det finnes en løpende forskudd registrert på barnet." +
                    "Det betyr at forskuddet har blitt opphørt. Fjerner forskudd periode fra"
            }
            barn.forskuddFra = null
            return
        }

        LOGGER.info {
            "Fant forskudd periode ${forskuddStønad.periodeFom()} - ${forskuddStønad.periodeTil()} " +
                "for barn med lagret forskudd periode ${barn.forskuddFra} - ${barn.forskuddTil} - ${barn.infoUtenPerioder()}"
        }
        if (forskuddStønad.periodeFom() != barn.forskuddFra || forskuddStønad.periodeTil() != barn.forskuddTil) {
            LOGGER.info {
                "Feil forskudd periode lagret for barn ${barn.infoUtenPerioder()}. Oppdaterer " +
                    "fra ${barn.forskuddFra} - ${barn.forskuddTil} til ${forskuddStønad.periodeFom()} - ${forskuddStønad.periodeTil()}"
            }
        }
        barn.forskuddFra = forskuddStønad.periodeFom()
        barn.forskuddTil = forskuddStønad.periodeTil()
    }

    private fun oppdaterBidrag(barn: Barn) {
        if (barn.skyldner == null) {
            LOGGER.info { "Barn ${barn.infoUtenPerioder()} har ingen skyldner, hopper over oppdatering av bidrag" }
            return
        }
        val historiskeBidrag =
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                barn.tilHentStønadHistoriskRequest(Stønadstype.BIDRAG),
            ) ?: run {
                LOGGER.info { "Fant ingen bidrag stønader for barn ${barn.infoMedPerioder()}" }
                return
            }

        if (historiskeBidrag.periodeListe.isEmpty()) {
            LOGGER.info { "Ingen bidrag perioder funnet for barn ${barn.infoMedPerioder()}" }
            return
        }

        LOGGER.info {
            "Fant bidrag periode ${historiskeBidrag.periodeFom()} - ${historiskeBidrag.periodeTil()} " +
                "for barn med lagret bidrag periode ${barn.bidragFra} - ${barn.bidragTil} - ${barn.infoUtenPerioder()}"
        }

        if (historiskeBidrag.periodeFom() != barn.bidragFra || historiskeBidrag.periodeTil() != barn.bidragTil) {
            LOGGER.info {
                "Feil bidrag periode lagret for barn ${barn.infoUtenPerioder()}. Oppdaterer " +
                    "fra ${barn.bidragFra} - ${barn.bidragTil} til ${historiskeBidrag.periodeFom()} - ${historiskeBidrag.periodeTil()}"
            }
        }
        barn.bidragFra = historiskeBidrag.periodeFom()
        barn.bidragTil = historiskeBidrag.periodeTil()
    }

    private fun StønadDto.periodeFom() =
        periodeListe
            .minBy { it.periode.fom }
            .periode.fom
            .atDay(1)

    private fun StønadDto.periodeTil() =
        periodeListe
            .maxBy { it.periode.fom }
            .periode.til
            ?.atDay(1)

    fun Barn.tilHentStønadHistoriskRequest(stønadstype: Stønadstype) =
        HentStønadHistoriskRequest(
            type = stønadstype,
            sak = Saksnummer(saksnummer),
            skyldner = if (stønadstype == Stønadstype.FORSKUDD) personidentNav else Personident(skyldner!!),
            kravhaver = Personident(kravhaver),
            gyldigTidspunkt = LocalDateTime.now(),
        )
}
