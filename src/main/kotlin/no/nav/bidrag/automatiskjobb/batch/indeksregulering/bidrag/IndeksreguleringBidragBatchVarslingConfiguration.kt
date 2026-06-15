package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag

import no.nav.bidrag.automatiskjobb.batch.utils.varsling.Batch
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchKategori
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Angir hvilke batcher for indeksregulering av bidrag som inngår i den månedlige slack-varslingen.
 * Cron-uttrykk for hver batch settes som miljøvariabler i nais-config.
 */
@Configuration
class IndeksreguleringBidragBatchVarslingConfiguration {
    @Bean
    fun indeksreguleringBidragBatchKategori(
        @Value($$"${INDEKSREGULERING_BIDRAG_OPPRETT_CRON:-}") opprettCron: String,
        @Value($$"${INDEKSREGULERING_BIDRAG_GJENNOMFOR_CRON:-}") gjennomførCron: String,
    ): BatchKategori =
        BatchKategori(
            navn = "Indeksregulering bidrag",
            batcher =
                listOf(
                    Batch("Opprett indeksregulering bidrag", opprettCron),
                    Batch("Gjennomfør indeksregulering bidrag", gjennomførCron),
                ),
        )
}