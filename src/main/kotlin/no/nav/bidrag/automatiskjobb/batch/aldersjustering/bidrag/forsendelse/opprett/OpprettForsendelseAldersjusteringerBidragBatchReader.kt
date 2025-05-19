package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.opprett

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.Collections

@Component
@StepScope
class OpprettForsendelseAldersjusteringerBidragBatchReader(
    forsendelseBestillingRepository: ForsendelseBestillingRepository,
) : RepositoryItemReader<ForsendelseBestilling>() {
    init {
        this.setRepository(forsendelseBestillingRepository)
        this.setMethodName("finnAlleForsendelserSomSkalOpprettesEllerSlettes")
        this.setPageSize(PAGE_SIZE)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
        this.isSaveState = false
    }
}
