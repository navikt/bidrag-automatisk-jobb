package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.StatusRepositoryItemReader
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.Collections

@Component
@StepScope
class SlettVedtaksforslagBatchReader(
    private val aldersjusteringRepository: AldersjusteringRepository,
) : StatusRepositoryItemReader<Aldersjustering>() {
    init {
        this.setRepository(aldersjusteringRepository)
        this.setMethodName("finnForStatus")
        this.setArguments(listOf(Status.SLETTES))
        this.setPageSize(PAGE_SIZE)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
        this.isSaveState = false
    }
}
