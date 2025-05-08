package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.opprett

import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.Collections

@Component
@StepScope
class OpprettBrevAldersjusteringerBidragBatchReader(
    forsendelseBestillingRepository: ForsendelseBestillingRepository,
) : RepositoryItemReader<ForsendelseBestilling>() {
    init {
        this.setRepository(forsendelseBestillingRepository)
        this.setMethodName("findAllByForsendelseIdIsNullAndSlettetTidspunktIsNull")
        this.setPageSize(100)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
    }
}
