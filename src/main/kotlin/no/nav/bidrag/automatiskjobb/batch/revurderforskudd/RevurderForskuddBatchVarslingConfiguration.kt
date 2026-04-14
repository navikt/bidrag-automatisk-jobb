package no.nav.bidrag.automatiskjobb.batch.revurderforskudd

import no.nav.bidrag.automatiskjobb.batch.utils.varsling.Batch
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchKategori
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Denne klassen angir hvilke batcher for revurdering forskudd som blir lagt til i den månedlige slack-varslingen.
 * Cron-uttrykk for hver batch settes som miljøvariabler i nais-config.
 */
@Configuration
class RevurderForskuddBatchVarslingConfiguration {
    @Bean
    fun revurderForskuddBatchKategori(
        @Value($$"${REVURDER_FORSKUDD_OPPRETT_CRON:-}") opprettCron: String,
        @Value($$"${REVURDER_FORSKUDD_EVALUER_CRON:-}") evaluerCron: String,
        @Value($$"${REVURDER_FORSKUDD_FATTE_VEDTAK_CRON:-}") fatteVedtakCron: String,
        @Value($$"${REVURDER_FORSKUDD_DISTRUBUER_FORSENDELSE_CRON:-}") distribuerForsendelseCron: String,
        @Value($$"${REVURDER_FORSKUDD_REVURDERINGSLENKE_CRON:-}") revurderingslenkeCron: String,
    ): BatchKategori =
        BatchKategori(
            navn = "Revurder forskudd",
            batcher =
                listOf(
                    Batch("Opprett revurdering forskudd", opprettCron),
                    Batch("Evaluer revurdering forskudd", evaluerCron),
                    Batch("Fatte vedtak om revurdering forskudd", fatteVedtakCron),
                    Batch("Distribuer forsendelse av vedtaksbrev", distribuerForsendelseCron),
                    Batch("Opprett revurderingslenke for tilbakekreving", revurderingslenkeCron),
                ),
        )
}