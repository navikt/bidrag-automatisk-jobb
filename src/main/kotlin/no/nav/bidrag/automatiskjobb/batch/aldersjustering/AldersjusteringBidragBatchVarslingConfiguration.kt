package no.nav.bidrag.automatiskjobb.batch.aldersjustering

import no.nav.bidrag.automatiskjobb.batch.utils.varsling.Batch
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchKategori
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Denne klassen angir hvilke batcher for aldersjustering av bidrag som blir lagt til i den månedlige slack-varslingen.
 * Cron-uttrykk for hver batch settes som miljøvariabler i nais-config.
 */
@Configuration
class AldersjusteringBidragBatchVarslingConfiguration {
    @Bean
    fun aldersjusteringBidragBatchKategori(
        @Value($$"${ALDERSJUSTERING_BIDRAG_OPPRETT_CRON:-}") opprettCron: String,
        @Value($$"${ALDERSJUSTERING_BIDRAG_BEREGN_CRON:-}") beregnCron: String,
        @Value($$"${ALDERSJUSTERING_BIDRAG_FATT_VEDTAK_CRON:-}") fattVedtakCron: String,
        @Value($$"${REVURDER_FORSKUDD_OPPRETT_FORSENDELSE_CRON:-}") opprettForsendelseCron: String,
        @Value($$"${REVURDER_FORSKUDD_DISTRUBUER_FORSENDELSE_CRON:-}") distribuerForsendelseCron: String,
        @Value($$"${ALDERSJUSTERING_BIDRAG_OPPRETT_OPPGAVE_CRON:-}") opprettOppgaveCron: String,
    ): BatchKategori =
        BatchKategori(
            navn = "Aldersjustering bidrag",
            batcher =
                listOf(
                    Batch("Opprett aldersjustering bidrag", opprettCron),
                    Batch("Beregner aldersjusteringer bidrag", beregnCron),
                    Batch("Fatte vedtak om aldersjustering bidrag", fattVedtakCron),
                    Batch("Opprett forsendelse av vedtaksbrev", opprettForsendelseCron),
                    Batch("Distribuer forsendelse av vedtaksbrev", distribuerForsendelseCron),
                    Batch("Opprette oppgave for aldersjustering bidrag", opprettOppgaveCron),
                ),
        )
}
