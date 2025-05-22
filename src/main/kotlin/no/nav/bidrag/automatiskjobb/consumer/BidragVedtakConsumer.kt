package no.nav.bidrag.automatiskjobb.consumer

import no.nav.bidrag.automatiskjobb.configuration.CacheConfiguration.Companion.VEDTAK_CACHE
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningVedtakConsumer
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.vedtak.request.HentVedtakForStønadRequest
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.behandling.vedtak.response.HentVedtakForStønadResponse
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
        get() = UriComponentsBuilder.fromUri(bidragVedtakUrl)

    fun hentVedtaksforslagBasertPåReferanase(referanse: String): VedtakDto? =
        postForEntity(
            bidragVedtakUri
                .pathSegment("vedtak")
                .pathSegment("unikreferanse")
                .build()
                .toUri(),
            referanse,
        )

    fun slettVedtaksforslag(vedtakId: Int) =
        deleteForEntity<Void>(
            bidragVedtakUri
                .pathSegment("vedtaksforslag")
                .pathSegment(vedtakId.toString())
                .build()
                .toUri(),
        )

    fun fatteVedtaksforslag(vedtakId: Int): Int =
        postForNonNullEntity(
            bidragVedtakUri
                // TODO: Endre dette til riktig url. Sikre at det ikke fattes vedtak under testing
                .pathSegment("vedtaksforslag2")
                .pathSegment(vedtakId.toString())
                .build()
                .toUri(),
            null,
        )

    fun oppdaterVedtaksforslag(
        vedtakId: Int,
        request: OpprettVedtakRequestDto,
    ): Int =
        putForEntity(
            bidragVedtakUri
                .pathSegment("vedtaksforslag")
                .pathSegment(vedtakId.toString())
                .build()
                .toUri(),
            request,
        )!!

    fun opprettVedtaksforslag(request: OpprettVedtakRequestDto): Int =
        postForNonNullEntity(
            bidragVedtakUri.pathSegment("vedtaksforslag").build().toUri(),
            request,
        )

    fun hentAlleVedtaksforslag(limit: Int): List<Int> =
        getForEntity(
            bidragVedtakUri
                .pathSegment("vedtaksforslag")
                .pathSegment("alle")
                .queryParam("limit", limit)
                .build()
                .toUri(),
        ) ?: emptyList()

    @Cacheable(VEDTAK_CACHE)
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentVedtak(vedtakId: Int): VedtakDto? =
        getForEntity(
            bidragVedtakUri
                .pathSegment("vedtak")
                .pathSegment(vedtakId.toString())
                .build()
                .toUri(),
        )

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentVedtakForStønad(request: HentVedtakForStønadRequest): HentVedtakForStønadResponse =
        postForNonNullEntity(
            bidragVedtakUri
                .pathSegment("vedtak")
                .pathSegment("hent-vedtak")
                .build()
                .toUri(),
            request,
        )
}
