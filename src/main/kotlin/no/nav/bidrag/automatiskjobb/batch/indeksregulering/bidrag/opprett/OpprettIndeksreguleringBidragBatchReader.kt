package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.core.listener.StepExecutionListener
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.data.domain.Sort
import java.time.LocalDate

class OpprettIndeksreguleringBidragBatchReader(
    private val barnRepository: BarnRepository,
    private val bidragFremTilDato: LocalDate,
    private val pageSize: Int,
) : ItemReader<Barn>,
    StepExecutionListener {
    private var saksnummer: List<String> = emptyList()

    private var delegate: RepositoryItemReader<Barn>? = null

    override fun beforeStep(stepExecution: StepExecution) {
        saksnummer =
            stepExecution.jobParameters
                .getString("saksnummer")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
    }

    private fun delegate(): RepositoryItemReader<Barn> = delegate ?: byggDelegate().also { delegate = it }

    private fun byggDelegate(): RepositoryItemReader<Barn> {
        val builder =
            RepositoryItemReaderBuilder<Barn>()
                .name("opprettIndeksreguleringBidragBatchReader")
                .repository(barnRepository)
                .saveState(false)
                .pageSize(pageSize)
                .sorts(mapOf("saksnummer" to Sort.Direction.ASC, "id" to Sort.Direction.ASC))

        return if (saksnummer.isEmpty()) {
            builder
                .methodName("findBarnMedLøpendeBidrag")
                .arguments(listOf(bidragFremTilDato))
                .build()
        } else {
            builder
                .methodName("finnBarnMedLøpendeBidragForSaksnummer")
                .arguments(listOf(bidragFremTilDato, saksnummer))
                .build()
        }
    }

    override fun read(): Barn? = delegate().read()
}
