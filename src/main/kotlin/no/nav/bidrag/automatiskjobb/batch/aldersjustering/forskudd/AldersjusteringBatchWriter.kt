package no.nav.bidrag.automatiskjobb.batch.aldersjustering.forskudd

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.item.data.RepositoryItemWriter
import org.springframework.stereotype.Component

@Component
class AldersjusteringBatchWriter(
    barnRepository: BarnRepository,
) : RepositoryItemWriter<Barn>() {
    init {
        this.setRepository(barnRepository)
        this.setMethodName("save")
    }
}
