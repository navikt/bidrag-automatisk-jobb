package no.nav.bidrag.automatiskjobb.batch.aldersjustering.forskudd

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class AldersjusteringForskuddBatchProcessor : ItemProcessor<Barn, Barn> {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(AldersjusteringForskuddBatchProcessor::class.java)
    }

    override fun process(barn: Barn): Barn {
        val transformedBarn = barn
        // TODO(Implementer logikk for aldersjustering)
        LOGGER.info("Omformer barn: $barn til $transformedBarn.")
        return barn
    }
}
