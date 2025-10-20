package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderingForskuddRepository
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger { }

@Component
class OpprettRevurderForskuddBatchProcessor(
    private val revurderingForskuddRepository: RevurderingForskuddRepository,
) : ItemProcessor<Barn, RevurderingForskudd> {
    private var batchId: String? = ""

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        batchId = stepExecution.jobParameters.getString("batchId")
    }

    override fun process(item: Barn): RevurderingForskudd {
        val revurderingForskudd =
            RevurderingForskudd(
                forMåned = YearMonth.now().toString(),
                batchId = batchId!!,
                barn = item,
                status = Status.UBEHANDLET,
            )
        LOGGER.info { "Opprettet revurdering forskudd for barn med id ${item.id}. $revurderingForskudd" }
        return revurderingForskuddRepository.save(revurderingForskudd)
    }
}
