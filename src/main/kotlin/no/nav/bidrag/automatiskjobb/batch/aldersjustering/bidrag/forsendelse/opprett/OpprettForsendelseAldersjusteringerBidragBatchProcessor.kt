package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.opprett

import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.service.ForsendelseBestillingService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
@StepScope
class OpprettForsendelseAldersjusteringerBidragBatchProcessor(
    private val forsendelseBestillingService: ForsendelseBestillingService,
) : ItemProcessor<ForsendelseBestilling, Unit> {
    private var prosesserFeilet: Boolean = false

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        prosesserFeilet = stepExecution.jobParameters.getString("prosesserFeilet").toBoolean()
    }

    override fun process(forsendelseBestilling: ForsendelseBestilling) {
        forsendelseBestillingService.opprettForsendelse(forsendelseBestilling, prosesserFeilet)
    }
}
