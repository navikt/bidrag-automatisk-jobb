package no.nav.bidrag.automatiskjobb.consumer

import no.nav.bidrag.automatiskjobb.configuration.CacheConfiguration.Companion.VEDTAK_CACHE
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningVedtakConsumer
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.vedtak.request.HentVedtakForStønadRequest
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.behandling.vedtak.response.HentVedtakForStønadResponse
import no.nav.bidrag.transport.behandling.vedtak.response.OpprettVedtakResponseDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragVedtakConsumer(
    @Value("\${BIDRAG_VEDTAK_URL}") private val bidragVedtakUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-vedtak"),
    BeregningVedtakConsumer {
    private val bidragVedtakUri
        get() = UriComponentsBuilder.fromUri(bidragVedtakUrl).pathSegment("vedtak")

    fun fatteVedtak(request: OpprettVedtakRequestDto): OpprettVedtakResponseDto =
        postForNonNullEntity(
            bidragVedtakUri.build().toUri(),
            request,
        )

    fun fatteVedtaksforslag(request: OpprettVedtakRequestDto): OpprettVedtakResponseDto =
        postForNonNullEntity(
            bidragVedtakUri.pathSegment("forslag").build().toUri(),
            request,
        )

    @Cacheable(VEDTAK_CACHE)
    override fun hentVedtak(vedtakId: Int): VedtakDto? =
        getForEntity(
            bidragVedtakUri.pathSegment(vedtakId.toString()).build().toUri(),
        )

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentVedtakForStønad(request: HentVedtakForStønadRequest): HentVedtakForStønadResponse =
        postForNonNullEntity(
            bidragVedtakUri.pathSegment("hent-vedtak").build().toUri(),
            request,
        )
}
