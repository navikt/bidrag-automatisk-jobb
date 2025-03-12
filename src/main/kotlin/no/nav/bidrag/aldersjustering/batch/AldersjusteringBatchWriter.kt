package no.nav.bidrag.aldersjustering.batch

import no.nav.bidrag.aldersjustering.persistence.entity.Barn
import no.nav.bidrag.aldersjustering.persistence.repository.BarnRepository
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
