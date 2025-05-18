package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.slettoppgave

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.Collections

@Component
@StepScope
class SlettOppgaveAldersjusteringerBidragBatchReader(
    aldersjusteringRepository: AldersjusteringRepository,
    @Value("#{jobParameters['barn']}") barn: String? = "",
    @Value("#{jobParameters['batchId']}") batchId: String,
) : RepositoryItemReader<Aldersjustering>() {
    init {
        val barnListe =
            barn
                ?.takeIf { it.isNotEmpty() }
                ?.split(",")
                ?.map { it.trim() }
                ?.map { Integer.valueOf(it) } ?: emptyList()
        this.setRepository(aldersjusteringRepository)

        this.setMethodName("finnOppgaveOpprettetForBarnOgBatchId")
        this.setArguments(listOf(barnListe, batchId))
        this.setPageSize(PAGE_SIZE)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
        this.isSaveState = false
    }
}
