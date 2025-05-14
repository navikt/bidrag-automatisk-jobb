package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.Collections

@Component
@StepScope
class FattVedtakOmAldersjusteringerBidragBatchReader(
    aldersjusteringRepository: AldersjusteringRepository,
    @Value("#{jobParameters['barn']}") barn: String? = "",
) : RepositoryItemReader<Aldersjustering>() {
    init {
        val barnListe = barn?.split(",")?.map { it.trim() }?.map { Integer.valueOf(it) } ?: emptyList()
        this.setRepository(aldersjusteringRepository)

        if (barnListe.isNotEmpty()) {
            this.setMethodName("finnForBarnBehandlingstypeOgStatus")
            this.setArguments(listOf(barnListe, listOf(Behandlingstype.FATTET_FORSLAG), listOf(Status.BEHANDLET)))
        } else {
            this.setMethodName("finnForBehandlingstypeOgStatus")
            this.setArguments(listOf(listOf(Behandlingstype.FATTET_FORSLAG), listOf(Status.BEHANDLET)))
        }
        this.setPageSize(PAGE_SIZE)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
        this.isSaveState = false
    }
}
