package no.nav.bidrag.automatiskjobb.batch.forsendelse.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import no.nav.bidrag.automatiskjobb.service.ForsendelseBestillingService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@StepScope
class OpprettForsendelseAldersjusteringerBidragBatchProcessor(
    private val forsendelseBestillingService: ForsendelseBestillingService,
    private val forsendelseBestillingRepository: ForsendelseBestillingRepository,
) : ItemProcessor<ForsendelseBestilling, Unit> {
    private var prosesserFeilet: Boolean = false

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        prosesserFeilet = stepExecution.jobParameters.getString("prosesserFeilet").toBoolean()
    }

    override fun process(forsendelseBestilling: ForsendelseBestilling) =
        try {
            forsendelseBestillingService.opprettForsendelse(forsendelseBestilling, prosesserFeilet)
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved opprettelse av forsendelse for bestilling ${forsendelseBestilling.id}" }
            forsendelseBestilling.feilBegrunnelse = e.message
            forsendelseBestillingRepository.save(forsendelseBestilling)
            null
        }
}
