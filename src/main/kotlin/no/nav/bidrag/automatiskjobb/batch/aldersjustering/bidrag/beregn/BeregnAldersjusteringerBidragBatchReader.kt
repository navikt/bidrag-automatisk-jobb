package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import no.nav.bidrag.automatiskjobb.batch.common.ModuloItemProcessor
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.util.Collections

@Component
@StepScope
class BeregnAldersjusteringerBidragBatchReader(
    aldersjusteringRepository: AldersjusteringRepository,
    @Value("#{stepExecutionContext['partitionNumber']}") private val partitionNumber: Int?,
    @Value("#{stepExecutionContext['gridSize']}") private val gridSize: Int?,
    transactionManager: PlatformTransactionManager,
) : ModuloItemProcessor<Aldersjustering>(partitionNumber, gridSize, transactionManager) {
    init {
        this.setRepository(aldersjusteringRepository)
        this.setMethodName("finnForFlereStatuser")
        this.setArguments(listOf(listOf(Status.UBEHANDLET, Status.FEILET, Status.SLETTET, Status.SIMULERT)))
        this.setPageSize(500)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
    }
}
