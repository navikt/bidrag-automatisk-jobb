package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.opprett

import no.nav.bidrag.automatiskjobb.batch.common.ModuloItemProcessor
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.util.Collections

@Component
@StepScope
class OpprettBrevAldersjusteringerBidragBatchReader(
    forsendelseBestillingRepository: ForsendelseBestillingRepository,
    @Value("#{stepExecutionContext['partitionNumber']}") private val partitionNumber: Int?,
    @Value("#{stepExecutionContext['gridSize']}") private val gridSize: Int?,
    transactionManager: PlatformTransactionManager,
) : ModuloItemProcessor<ForsendelseBestilling>(partitionNumber, gridSize, transactionManager) {
    init {
        this.setRepository(forsendelseBestillingRepository)
        this.setMethodName("findAllByForsendelseIdIsNullAndSlettetTidspunktIsNull")
        this.setPageSize(100)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
    }
}
