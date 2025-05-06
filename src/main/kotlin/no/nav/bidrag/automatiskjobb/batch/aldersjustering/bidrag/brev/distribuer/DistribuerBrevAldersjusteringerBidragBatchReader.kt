package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.distribuer

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.Collections

@Component
@StepScope
class DistribuerBrevAldersjusteringerBidragBatchReader(
    aldersjusteringRepository: AldersjusteringRepository,
) : RepositoryItemReader<Aldersjustering>() {
    init {
        this.setRepository(aldersjusteringRepository)
        this.setMethodName("findAllByVedtakforselselseIdIsNotNullAndVedtakjournalpostIdIsNull")
        this.setPageSize(100)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
    }
}
