package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.data.domain.Sort

/**
 * Custom [ItemReader] som grupperer [Barn]-rader per saksnummer før de sendes til prosessoren.
 *
 * Bakgrunn:
 * En sak kan ha flere barn som alle mottar forskudd. [OpprettRevurderForskuddBatchProcessor]
 * forventer å motta alle barn tilhørende samme sak samlet i én liste, slik at det kan opprettes
 * én [no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd] per sak – ikke per barn.
 *
 * Hvordan det fungerer:
 * Den underliggende [RepositoryItemReader] (delegaten) leser én og én [Barn] fra databasen,
 * sortert på saksnummer og deretter id. Denne readeren "kikker ett steg fremover" (peek-mønster)
 * for å oppdage når saksnummeret skifter, og returnerer da den innsamlede gruppen til prosessoren.
 * Det peekede barnet lagres til neste kall av [read], slik at ingen barn går tapt mellom sider.
 */
class OpprettRevurderForskuddBatchReader(
    barnRepository: BarnRepository,
    forskuddFremTilDato: java.time.LocalDate,
    pageSize: Int,
) : ItemReader<List<Barn>> {
    /**
     * Den underliggende Spring Batch-readeren som henter [Barn] fra databasen side for side.
     * Sortering på saksnummer + id sikrer at alle barn i samme sak alltid er konsekutive,
     * noe som er en forutsetning for grupperingslogikken under.
     */
    private val delegate: RepositoryItemReader<Barn> =
        RepositoryItemReaderBuilder<Barn>()
            .name("opprettRevurderForskuddBatchReader")
            .repository(barnRepository)
            .methodName("findBarnSomSkalRevurdereForskudd")
            .arguments(listOf(forskuddFremTilDato))
            .saveState(false)
            .pageSize(pageSize)
            .sorts(mapOf("saksnummer" to Sort.Direction.ASC, "id" to Sort.Direction.ASC))
            .build()

    /**
     * Buffer for det første barnet i neste sak. Når vi oppdager at saksnummeret har skiftet,
     * lagres det nye barnet her slik at det ikke kastes, men brukes i neste kall til [read].
     */
    private var peeked: Barn? = null

    /**
     * Flagg som settes når delegaten ikke har flere rader å lese.
     * Hindrer unødvendige kall til delegaten etter at alle rader er behandlet.
     */
    private var exhausted = false

    /**
     * Returnerer alle barn for én sak som én liste, eller `null` når alle saker er behandlet.
     *
     * Algoritme:
     * 1. Hent første barn fra buffer (peek) eller delegaten. Returner `null` om ingen flere fins.
     * 2. Samle alle påfølgende barn med samme saksnummer i en gruppe.
     * 3. Når saksnummeret skifter, lagre det nye barnet i bufferet og returner den ferdige gruppen.
     */
    override fun read(): List<Barn>? {
        if (exhausted) return null

        // Hent første barn i gruppen – enten fra forrige peek eller direkte fra delegaten
        val first = peeked ?: delegate.read() ?: return null
        peeked = null

        val gruppe = mutableListOf(first)
        val saksnummer = first.saksnummer

        while (true) {
            val neste = delegate.read()
            when {
                // Ingen flere rader – merk som ferdig og returner siste gruppe
                neste == null -> {
                    exhausted = true
                    break
                }

                // Samme sak – legg til i pågående gruppe
                neste.saksnummer == saksnummer -> {
                    gruppe.add(neste)
                }

                // Nytt saksnummer – lagre barnet til neste kall og avslutt gruppen
                else -> {
                    peeked = neste
                    break
                }
            }
        }

        return gruppe
    }
}
