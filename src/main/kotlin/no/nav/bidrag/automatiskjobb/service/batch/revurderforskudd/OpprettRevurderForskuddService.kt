package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBehandlingConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.beregn.barnebidrag.service.VedtakService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
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
        barn: Barn,
        batchId: String,
        cutoffTidspunktForManueltVedtak: LocalDateTime,
    ): RevurderingForskudd? {
        val inneværendeMåned = YearMonth.now().toString()
        if (finnesEksisterendeRevurderingForskudd(barn, inneværendeMåned)) {
            LOGGER.info { "Barn ${barn.kravhaver} har allerede revurdering av forskudd for måned $inneværendeMåned. Oppretter ikke ny revurdering." }
            return null
        }
        if (harÅpentForskuddssak(barn)) {
            LOGGER.info { "Barn ${barn.kravhaver} har åpent forskuddssak. Oppretter ikke revurdering av forskudd." }
            return null
        }
        val sisteManuelleVedtakTidspunkt = hentSisteManuelleVedtakTidspunkt(barn)
        if (sisteManuelleVedtakTidspunkt != null && sisteManuelleVedtakTidspunkt.isAfter(cutoffTidspunktForManueltVedtak)) {
            LOGGER.info {
                "Barn ${barn.kravhaver} har manuelt vedtak opprettet $sisteManuelleVedtakTidspunkt etter cutoff tidspunkt $cutoffTidspunktForManueltVedtak. Oppretter ikke revurdering av forskudd."
            }
            return null
        }
        return RevurderingForskudd(
            forMåned = inneværendeMåned,
            batchId = batchId,
            barn = barn,
            status = Status.UBEHANDLET,
        ).also {
            LOGGER.info {
                "Opprettet revurdering forskudd for barn med id ${barn.id}. $it"
            }
        }
    }

    private fun finnesEksisterendeRevurderingForskudd(
        barn: Barn,
        inneværendeMåned: String
    ): Boolean {
        return revurderForskuddRepository.findAllByBarnIdAndForMåned(barn.id!!, inneværendeMåned) != null
    }

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
                    Personident(barn.skyldner ?: ""),
                    Saksnummer(barn.saksnummer),
                ),
            )?.vedtak
            ?.opprettetTidspunkt
}
