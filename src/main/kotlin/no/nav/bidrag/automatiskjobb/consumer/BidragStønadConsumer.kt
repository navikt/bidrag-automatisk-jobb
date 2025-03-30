package no.nav.bidrag.automatiskjobb.consumer

import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningStønadConsumer
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.stonad.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.stonad.request.SkyldnerStønaderRequest
import no.nav.bidrag.transport.behandling.stonad.response.SkyldnerStønaderResponse
import no.nav.bidrag.transport.behandling.stonad.response.StønadDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragStønadConsumer(
    @Value("\${BIDRAG_STONAD_URL}") private val bidragStønadUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-stønad"),
    BeregningStønadConsumer {
    private val bidragsStønadUri
        get() = UriComponentsBuilder.fromUri(bidragStønadUrl)

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun hentAlleStønaderForBidragspliktig(personidentBidragspliktig: Personident): SkyldnerStønaderResponse =
        postForNonNullEntity(
            bidragsStønadUri.pathSegment("hent-alle-stonader-for-skyldner").build().toUri(),
            SkyldnerStønaderRequest(personidentBidragspliktig),
        )

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentHistoriskeStønader(request: HentStønadHistoriskRequest): StønadDto? =
        postForEntity(
            bidragsStønadUri.pathSegment("hent-stonad-historisk/").build().toUri(),
            request,
        )
}
