package no.nav.bidrag.automatiskjobb.batch.bidrag

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
class AldersjusteringBidragBatchReader(
    barnRepository: BarnRepository,
    @Value("#{jobParameters['forDato']}") forDato: LocalDate,
    @Value("#{jobParameters['kjøredato']}") kjøredato: LocalDate,
) : RepositoryItemReader<Barn>() {
    init {
        this.setRepository(barnRepository)
        this.setMethodName("finnBarnSomSkalAldersjusteresForÅr")
        this.setArguments(listOf(forDato.year, kjøredato))
        this.setPageSize(100)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
    }
}
