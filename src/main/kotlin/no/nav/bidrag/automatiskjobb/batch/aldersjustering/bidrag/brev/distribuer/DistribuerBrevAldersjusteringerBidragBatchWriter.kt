package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.distribuer

import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.service.ForsendelseBestillingService
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
@StepScope
class DistribuerBrevAldersjusteringerBidragBatchWriter(
    private val forsendelseBestillingService: ForsendelseBestillingService,
) : ItemWriter<ForsendelseBestilling> {
    override fun write(chunk: Chunk<out ForsendelseBestilling>) {
        chunk.forEach { forsendelseBestilling ->
            forsendelseBestillingService.distribuerForsendelse(forsendelseBestilling)
        }
    }
}
