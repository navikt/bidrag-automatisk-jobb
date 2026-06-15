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

/**
 * Custom [ItemReader] som grupperer [Barn]-rader per saksnummer før de sendes til prosessoren.
 *
 * Bakgrunn:
 * En sak kan ha flere barn som alle mottar bidrag. [OpprettIndeksreguleringBidragBatchProcessor]
 * forventer å motta alle barn tilhørende samme sak samlet i én liste, slik at det kan opprettes
 * én [no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering] per sak – ikke per barn.
 *
 * Utvalg:
 * Det hentes barn som har et løpende bidrag. Dersom det er angitt en liste med saksnummer
 * (via job-parameteren "saksnummer"), begrenses utvalget til disse sakene.
 *
 * Gruppering:
 * Den underliggende [RepositoryItemReader] (delegaten) leser én og én [Barn] fra databasen,
 * sortert på saksnummer og deretter id. Denne readeren "kikker ett steg fremover" (peek-mønster)
 * for å oppdage når saksnummeret skifter, og returnerer da den innsamlede gruppen til prosessoren.
 */
class OpprettIndeksreguleringBidragBatchReader(
    private val barnRepository: BarnRepository,
    private val bidragFremTilDato: LocalDate,
    private val pageSize: Int,
) : ItemReader<List<Barn>>,
    StepExecutionListener {
    private var saksnummer: List<String> = emptyList()

    private var delegate: RepositoryItemReader<Barn>? = null

    private var peeked: Barn? = null

    private var exhausted = false

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
                .methodName("findBarnSomSkalIndeksregulereBidrag")
                .arguments(listOf(bidragFremTilDato))
                .build()
        } else {
            builder
                .methodName("finnBarnMedLøpendeBidragForSaksnummer")
                .arguments(listOf(bidragFremTilDato, saksnummer))
                .build()
        }
    }

    override fun read(): List<Barn>? {
        if (exhausted) return null

        val first = peeked ?: delegate().read() ?: return null
        peeked = null

        val gruppe = mutableListOf(first)
        val saksnummer = first.saksnummer

        while (true) {
            val neste = delegate().read()
            when {
                neste == null -> {
                    exhausted = true
                    break
                }

                neste.saksnummer == saksnummer -> {
                    gruppe.add(neste)
                }

                else -> {
                    peeked = neste
                    break
                }
            }
        }

        return gruppe
    }
}
