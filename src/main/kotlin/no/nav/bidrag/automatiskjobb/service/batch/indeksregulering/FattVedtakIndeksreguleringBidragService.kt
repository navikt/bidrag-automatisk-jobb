package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

private val LOGGER = KotlinLogging.logger { }

@Service
class FattVedtakIndeksreguleringBidragService(
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val indeksreguleringRepository: IndeksreguleringRepository,
) {
    fun fattVedtak(
        indeksregulering: Indeksregulering,
        simuler: Boolean,
    ) {
        if (simuler) {
            LOGGER.info {
                "Simulering: Fatter ikke vedtak om indeksregulering av bidrag for indeksregulering " +
                    "${indeksregulering.id} i sak ${indeksregulering.barn.saksnummer}."
            }
            return
        }

        LOGGER.info {
            "Fatter vedtak om indeksregulering av bidrag for indeksregulering ${indeksregulering.id} " +
                "med vedtaksid ${indeksregulering.vedtak} i sak ${indeksregulering.barn.saksnummer}."
        }
        try {
            bidragVedtakConsumer.fatteVedtaksforslag(
                indeksregulering.vedtak ?: error("Indeksregulering ${indeksregulering.id} mangler vedtak!"),
            )
            indeksregulering.status = Status.FATTET
            indeksregulering.fattetTidspunkt = Timestamp(System.currentTimeMillis())
            indeksreguleringRepository.save(indeksregulering)
        } catch (e: Exception) {
            LOGGER.error(e) {
                "Feil ved fatting av vedtak om indeksregulering av bidrag for indeksregulering " +
                    "${indeksregulering.id} i sak ${indeksregulering.barn.saksnummer}."
            }
            indeksregulering.status = Status.FATTE_VEDTAK_FEILET
            indeksreguleringRepository.save(indeksregulering)
            throw e
        }
    }
}
