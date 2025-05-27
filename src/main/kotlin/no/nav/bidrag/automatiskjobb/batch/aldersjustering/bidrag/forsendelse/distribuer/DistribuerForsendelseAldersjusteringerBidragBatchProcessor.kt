package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.distribuer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import no.nav.bidrag.automatiskjobb.service.ForsendelseBestillingService
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@StepScope
class DistribuerForsendelseAldersjusteringerBidragBatchProcessor(
    private val forsendelseBestillingService: ForsendelseBestillingService,
    private val forsendelseBestillingRepository: ForsendelseBestillingRepository,
) : ItemProcessor<ForsendelseBestilling, Unit> {
    override fun process(forsendelseBestilling: ForsendelseBestilling) =
        try {
            forsendelseBestillingService.distribuerForsendelse(forsendelseBestilling)
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved distribusjon av forsendelse ${forsendelseBestilling.forsendelseId}" }
            forsendelseBestilling.feilBegrunnelse = "Distribusjon feilet: ${e.message}"
            forsendelseBestillingRepository.save(forsendelseBestilling)
            null
        }
}
