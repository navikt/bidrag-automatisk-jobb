package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@StepScope
class OpprettAldersjusteringerBidragBatchProcessor(
    private val aldersjusteringService: AldersjusteringService,
) : ItemProcessor<Barn, Aldersjustering?> {
    private var år: Long? = -1
    private var batchId: String? = ""

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        batchId = stepExecution.jobParameters.getString("batchId")
        år = stepExecution.jobParameters.getLong("år")
    }

    override fun process(barn: Barn): Aldersjustering? =
        try {
            aldersjusteringService.opprettAldersjusteringForBarn(barn, år!!.toInt(), batchId!!, Stønadstype.BIDRAG)
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved opprettelse av aldersjustering for barn ${barn.id}" }
            null
        }
}
