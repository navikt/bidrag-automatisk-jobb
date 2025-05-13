package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave

import no.nav.bidrag.automatiskjobb.batch.common.ModuloItemProcessor
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Behandlingstype
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
class OppgaveAldersjusteringerBidragBatchReader(
    aldersjusteringRepository: AldersjusteringRepository,
    @Value("#{jobParameters['barn']}") barn: String? = "",
    @Value("#{stepExecutionContext['partitionNumber']}") private val partitionNumber: Int?,
    @Value("#{stepExecutionContext['gridSize']}") private val gridSize: Int?,
    transactionManager: PlatformTransactionManager,
) : ModuloItemProcessor<Aldersjustering>(partitionNumber, gridSize, transactionManager) {
    init {
        val barnListe = barn?.split(",")?.map { it.trim() }?.map { Integer.valueOf(it) } ?: emptyList()
        this.setRepository(aldersjusteringRepository)

        if (barnListe.isNotEmpty()) {
            this.setMethodName("finnForBarnBehandlingstypeOgStatus")
            this.setArguments(listOf(barnListe, listOf(Behandlingstype.MANUELL), listOf(Status.BEHANDLET)))
        } else {
            this.setMethodName("finnForBehandlingstypeOgStatus")
            this.setArguments(listOf(listOf(Behandlingstype.MANUELL), listOf(Status.BEHANDLET)))
        }
        this.setPageSize(100)
        this.setSort(Collections.singletonMap("id", Sort.Direction.ASC))
        this.isSaveState = false
    }
}
