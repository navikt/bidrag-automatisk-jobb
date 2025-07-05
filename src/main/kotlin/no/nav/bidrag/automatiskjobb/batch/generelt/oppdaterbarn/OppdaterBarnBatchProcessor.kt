package no.nav.bidrag.automatiskjobb.batch.generelt.oppdaterbarn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.service.BarnService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@StepScope
class OppdaterBarnBatchProcessor(
    private val barnService: BarnService,
) : ItemProcessor<Barn, Unit> {
    private var simuler: Boolean = true

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        simuler = stepExecution.jobParameters.getString("simuler").toBoolean()
    }

    override fun process(barn: Barn) =
        try {
            barnService.oppdaterBarnForskuddOgBidragPerioder(barn, simuler)
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved oppdatering av barn ${barn.id}" }
            null
        }
}
