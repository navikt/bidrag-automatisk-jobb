package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.distribuer

import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.service.ForsendelseBestillingService
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
@StepScope
class DistribuerBrevAldersjusteringerBidragBatchProcessor(
    private val forsendelseBestillingService: ForsendelseBestillingService,
) : ItemProcessor<ForsendelseBestilling, Unit> {
    override fun process(forsendelseBestilling: ForsendelseBestilling) {
        forsendelseBestillingService.distribuerForsendelse(forsendelseBestilling)
    }
}
