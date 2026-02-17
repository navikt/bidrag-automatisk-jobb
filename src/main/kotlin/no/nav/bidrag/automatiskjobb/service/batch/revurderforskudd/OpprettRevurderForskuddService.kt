package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBehandlingConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.felles.toLocalDate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger { }

@Service
class OpprettRevurderForskuddService(
    private val bidragBehandlingConsumer: BidragBehandlingConsumer,
    private val vedtakService: VedtakService,
    private val revurderForskuddRepository: RevurderForskuddRepository,
) {
    fun opprettRevurdereForskudd(
        barn: List<Barn>,
        batchId: String,
        cutoffTidspunktForManueltVedtak: LocalDateTime,
    ): RevurderingForskudd? {
        val inneværendeMåned = YearMonth.now()
        if (finnesEksisterendeRevurderingForskudd(barn.first().saksnummer, inneværendeMåned)) {
            LOGGER.info {
                "Sak ${barn.first().saksnummer} har allerede revurdering av forskudd for måned $inneværendeMåned. Oppretter ikke ny revurdering."
            }
            return null
        }
        if (harÅpentForskuddssak(barn.first())) {
            LOGGER.info { "Sak ${barn.first().saksnummer} har åpent forskuddssak. Oppretter ikke revurdering av forskudd." }
            return null
        }

        val sisteManuelleVedtakTidspunkt = hentSisteManuelleVedtakTidspunkt(barn.first())
        if (sisteManuelleVedtakTidspunkt != null && sisteManuelleVedtakTidspunkt.isAfter(cutoffTidspunktForManueltVedtak)) {
            LOGGER.info {
                "Sak ${barn.first().saksnummer} har manuelt vedtak opprettet $sisteManuelleVedtakTidspunkt etter cutoff tidspunkt $cutoffTidspunktForManueltVedtak. Oppretter ikke revurdering av forskudd."
            }
            return null
        }

        val barnFiltrert = barn.filterNot { erBarnAttenFørNesteMåned(it, inneværendeMåned) }.toMutableList()
        if (barnFiltrert.isEmpty()) {
            LOGGER.info {
                "Sak ${barn.first().saksnummer} har ingen barn som ikke er over 18 år eller fyller 18 inneværende måned. Oppretter ikke revurdering av forskudd."
            }
            return null
        }
        return RevurderingForskudd(
            forMåned = inneværendeMåned.toString(),
            batchId = batchId,
            barn = barnFiltrert,
            saksnummer = barnFiltrert.first().saksnummer,
            status = Status.UBEHANDLET,
        ).also {
            LOGGER.info {
                "Opprettet revurdering forskudd for sak ${barnFiltrert.first().saksnummer}. $it"
            }
        }
    }

    private fun erBarnAttenFørNesteMåned(
        barn: Barn,
        inneværendeMåned: YearMonth,
    ): Boolean {
        val erAttenFørNesteMåned = barn.fødselsdato?.plusYears(18)?.isBefore(inneværendeMåned.toLocalDate().plusMonths(1)) == true
        if (erAttenFørNesteMåned) {
            LOGGER.info {
                "Barn ${barn.kravhaver} er over 18 år eller fyller 18 inneværende måned. Tar ikke med barn."
            }
        }
        return erAttenFørNesteMåned
    }

    private fun finnesEksisterendeRevurderingForskudd(
        saksnummer: String,
        inneværendeMåned: YearMonth,
    ): Boolean = revurderForskuddRepository.findAllBySaksnummerAndForMåned(saksnummer, inneværendeMåned.toString()) != null

    private fun harÅpentForskuddssak(barn: Barn): Boolean {
        val hentÅpneBehandlingerRespons = bidragBehandlingConsumer.hentÅpneBehandlingerForBarn(barn.kravhaver)
        return hentÅpneBehandlingerRespons.behandlinger.any { behandling -> behandling.stønadstype == Stønadstype.FORSKUDD }
    }

    private fun hentSisteManuelleVedtakTidspunkt(barn: Barn): LocalDateTime? =
        vedtakService
            .finnSisteManuelleVedtak(
                Stønadsid(
                    Stønadstype.FORSKUDD,
                    Personident(barn.kravhaver),
                    Personident("NAV"),
                    Saksnummer(barn.saksnummer),
                ),
            )?.vedtak
            ?.opprettetTidspunkt
}
