package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.lagreb4

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import no.nav.bidrag.automatiskjobb.service.ReskontroService
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component
import java.math.BigDecimal

private val LOGGER = KotlinLogging.logger {}

@Component
@StepScope
class LagreB4InformasjonBidragBatchProcessor(
    private val reskontroService: ReskontroService,
    private val vedtakService: VedtakService,
    private val aldersjusteringRepository: AldersjusteringRepository,
) : ItemProcessor<Aldersjustering, Unit> {
    override fun process(aldersjustering: Aldersjustering): Unit? {
        val stønadsid = aldersjustering.barn.tilStønadsid(aldersjustering.stønadstype)
        val vedtaksid =
            aldersjustering.vedtak ?: run {
                LOGGER.warn { "Aldersjustering ${aldersjustering.id} mangler vedtaksid – hopper over" }
                return null
            }
        val vedtak = vedtakService.hentVedtak(vedtaksid)
        val vedtakstidspunkt =
            vedtak?.vedtakstidspunkt?.toLocalDate() ?: run {
                LOGGER.warn { "Vedtak ${vedtak?.vedtaksid} mangler vedtakstidspunkt – hopper over" }
                return null
            }

        val b4Beløp = reskontroService.hentSumAvregningForStønad(stønadsid, vedtakstidspunkt)
        if (b4Beløp > BigDecimal.ZERO) {
            aldersjustering.b4Beløp = b4Beløp
            aldersjusteringRepository.save(aldersjustering)
            LOGGER.info { "Lagret b4Beløp=$b4Beløp for aldersjustering ${aldersjustering.id} (stønad=$stønadsid)" }
        }
        return null
    }
}
