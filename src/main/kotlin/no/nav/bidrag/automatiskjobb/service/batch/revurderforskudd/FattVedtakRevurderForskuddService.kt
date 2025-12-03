package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Forsendelsestype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.automatiskjobb.service.ForsendelseBestillingService
import org.springframework.stereotype.Service
import java.sql.Timestamp

private val LOGGER = KotlinLogging.logger { }

@Service
class FattVedtakRevurderForskuddService(
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val revurderForskuddRepository: RevurderForskuddRepository,
    private val forsendelseBestillingService: ForsendelseBestillingService,
) {
    fun fattVedtak(
        revurderingForskudd: RevurderingForskudd,
        simuler: Boolean,
    ) {
        if (simuler) {
            LOGGER.info {
                "Simulering: Fatter vedtak om revurdering av forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer}."
            }
            return
        }

        LOGGER.info {
            "Fatter vedtak om revurdering av forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer}."
        }
        try {
            bidragVedtakConsumer.fatteVedtaksforslag(
                revurderingForskudd.vedtak ?: error("RevurderForskudd ${revurderingForskudd.id} mangler vedtak!"),
            )
            revurderingForskudd.status = Status.FATTET
            revurderingForskudd.fattetTidspunkt = Timestamp(System.currentTimeMillis())
        } catch (e: Exception) {
            LOGGER.error(e) {
                "Feil ved fatting av vedtak om revurdering av forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer}."
            }
            revurderingForskudd.status = Status.FATTE_VEDTAK_FEILET
            revurderForskuddRepository.save(revurderingForskudd)
            throw e
        }

        if (revurderingForskudd.status == Status.FATTET) {
            val forsendelseBestillinger =
                forsendelseBestillingService.opprettBestilling(
                    revurderingForskudd,
                    Forsendelsestype.REVURDERING_FORSKUDD,
                )
            revurderingForskudd.forsendelseBestilling.addAll(forsendelseBestillinger)
        }
        revurderForskuddRepository.save(revurderingForskudd)
    }
}
