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
        if (simuler) {
            LOGGER.info {
                "Simuleringmodus er på. Gjør ingen endring på periodene for forskudd/bidrag for barn ${barn.infoUtenPerioder()}"
            }
            return
        }
        barn.oppdatert = LocalDateTime.now()
        barnRepository.save(barn)
    }

    private fun oppdaterForskudd(barn: Barn) {
        val forskuddStønad =
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                barn.tilHentStønadHistoriskRequest(Stønadstype.FORSKUDD),
            ) ?: run {
                LOGGER.info { "Fant ingen forskudd stønader for barn ${barn.infoMedPerioder()}" }
                return
            }

        if (forskuddStønad.periodeListe.isEmpty()) {
            LOGGER.info { "Ingen forskudd perioder funnet for barn ${barn.infoMedPerioder()}" }
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
