package no.nav.bidrag.automatiskjobb.batch.indeksregulering.forskudd

import no.nav.bidrag.automatiskjobb.batch.utils.varsling.Batch
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchKategori
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Angir hvilke batcher for indeksregulering av forskudd som inngår i den månedlige slack-varslingen.
 * Cron-uttrykk for hver batch settes som miljøvariabler i nais-config.
 */
@Configuration
class IndeksreguleringForskuddBatchVarslingConfiguration {
    @Bean
    fun indeksreguleringForskuddBatchKategori(
        @Value($$"${INDEKSREGULERING_FORSKUDD_OPPRETT_CRON:-}") opprettCron: String,
    ): BatchKategori =
        BatchKategori(
            navn = "Indeksregulering forskudd",
            batcher =
                listOf(
                    Batch("Opprett indeksregulering forskudd", opprettCron),
                ),
        )
}