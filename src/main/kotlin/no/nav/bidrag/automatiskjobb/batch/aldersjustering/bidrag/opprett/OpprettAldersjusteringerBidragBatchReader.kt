package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.batch.common.ModuloItemProcessor
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.util.Collections

@Component
@StepScope
class OpprettAldersjusteringerBidragBatchReader(
    @Value("#{jobParameters['kjøredato']}") kjøredato: LocalDate? = LocalDate.now(),
    @Value("#{jobParameters['år']}") år: Long? = -1,
    barnRepository: BarnRepository,
    @Value("#{stepExecutionContext['partitionNumber']}") private val partitionNumber: Int?,
    @Value("#{stepExecutionContext['gridSize']}") private val gridSize: Int?,
    transactionManager: PlatformTransactionManager,
) : ModuloItemProcessor<Barn>(partitionNumber, gridSize, transactionManager) {
    init {
        this.setRepository(barnRepository)
        this.setMethodName("finnBarnSomSkalAldersjusteresForÅr")
        this.setArguments(listOf(år?.toInt(), kjøredato))
        this.setPageSize(500)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
        this.isSaveState = false
    }
}
