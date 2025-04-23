package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprettaldersjustering

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.Collections

@Component
@StepScope
class AldersjusteringBidragOpprettAldersjusteringerBatchReader(
    @Value("#{jobParameters['kjøredato']}") kjøredato: LocalDate? = LocalDate.now(),
    @Value("#{jobParameters['år']}") år: Long? = -1,
    barnRepository: BarnRepository,
) : RepositoryItemReader<Barn>() {
    init {
        this.setRepository(barnRepository)
        this.setMethodName("finnBarnSomSkalAldersjusteresForÅr")
        this.setArguments(listOf(år?.toInt(), kjøredato))
        this.setPageSize(100)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
    }
}
