package no.nav.bidrag.automatiskjobb.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.behandling.HentÅpneBehandlingerRequest
import no.nav.bidrag.transport.behandling.behandling.HentÅpneBehandlingerRespons
import no.nav.bidrag.transport.behandling.grunnlag.request.HentGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
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
class BidragGrunnlagConsumer(
    @param:Value($$"${BIDRAG_GRUNNLAG_URL}") val url: URI,
    @param:Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-grunnlag") {
    private fun createUri(path: String?) =
        UriComponentsBuilder
            .fromUri(url)
            .path(path ?: "")
            .build()
            .toUri()

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    fun hentGrunnlag(hentGrunnlagRequestDto: HentGrunnlagRequestDto): HentGrunnlagDto =
        try {
            postForNonNullEntity(
                createUri("/hentgrunnlag"),
                hentGrunnlagRequestDto,
            )
        } catch (
            e: HttpStatusCodeException,
        ) {
            LOGGER.error(e) { "Det skjedde en feil ved henting av grunnlag. Request: $hentGrunnlagRequestDto" }
            throw e
        }
}
