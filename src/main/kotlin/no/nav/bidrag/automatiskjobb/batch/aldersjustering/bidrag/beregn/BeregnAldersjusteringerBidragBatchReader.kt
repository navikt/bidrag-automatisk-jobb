package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.Collections

@Component
@StepScope
class BeregnAldersjusteringerBidragBatchReader(
    aldersjusteringRepository: AldersjusteringRepository,
) : RepositoryItemReader<Aldersjustering>() {
    init {
        this.setRepository(aldersjusteringRepository)
        this.setMethodName("finnForFlereStatuser")
        this.setArguments(listOf(listOf(Status.UBEHANDLET, Status.FEILET, Status.SLETTET, Status.SIMULERT)))
        this.setPageSize(PAGE_SIZE)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
        this.isSaveState = false
    }
}
