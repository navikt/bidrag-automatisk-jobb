package no.nav.bidrag.automatiskjobb.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.behandling.HentÅpneBehandlingerRequest
import no.nav.bidrag.transport.behandling.behandling.HentÅpneBehandlingerRespons
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val LOGGER = KotlinLogging.logger { }

@Service
class BidragBehandlingConsumer(
    @param:Value($$"${BIDRAG_BEHANDLING_URL}") val url: URI,
    @param:Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-behandling") {
    private fun createUri(path: String?) =
        UriComponentsBuilder
            .fromUri(url)
            .path(path ?: "")
            .build()
            .toUri()

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    fun hentÅpneBehandlingerForBarn(barnIdent: String): HentÅpneBehandlingerRespons =
        try {
            postForNonNullEntity(
                createUri("/api/v2/behandling/apnebehandlinger"),
                HentÅpneBehandlingerRequest(barnIdent),
            )
        } catch (
            e: HttpStatusCodeException,
        ) {
            LOGGER.error(e) { "Det skjedde en feil ved henting av åpne behandlinger for barn $barnIdent" }
            throw e
        }
}
