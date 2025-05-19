package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@StepScope
class SlettVedtaksforslagBatchInnlesingProcessor :
    ItemProcessor<Aldersjustering, Aldersjustering>,
    StepExecutionListener {
    lateinit var stepExecution: StepExecution

    override fun process(item: Aldersjustering): Aldersjustering {
        stepExecution.jobExecution.executionContext.put("slettVedtaksforslagId", item.id)
        return item
    }

    override fun beforeStep(stepExecution: StepExecution) {
        this.stepExecution = stepExecution
    }
}
