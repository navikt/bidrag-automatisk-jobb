package no.nav.bidrag.automatiskjobb.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.configuration.CacheConfiguration.Companion.VEDTAK_CACHE
import no.nav.bidrag.automatiskjobb.service.model.OpprettVedtakConflictResponse
import no.nav.bidrag.automatiskjobb.utils.JsonUtil.Companion.tilJson
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningVedtakConsumer
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.vedtak.request.HentManuelleVedtakRequest
import no.nav.bidrag.transport.behandling.vedtak.request.HentVedtakForStønadRequest
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.behandling.vedtak.response.HentVedtakForStønadResponse
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val LOGGER = KotlinLogging.logger { }

@Component
class BidragVedtakConsumer(
    @param:Value("\${BIDRAG_VEDTAK_URL}") private val bidragVedtakUrl: URI,
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
                .pathSegment("vedtaksforslag")
                .pathSegment(vedtakId.toString())
                .build()
                .toUri(),
            null,
        )

    fun opprettEllerOppdaterVedtaksforslag(request: OpprettVedtakRequestDto) =
        try {
            slettEksisterendeVedtaksforslag(request.unikReferanse!!)
            LOGGER.info { "Oppretter vedtaksforslag: ${tilJson(request)}" }
            opprettVedtaksforslag(request)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                LOGGER.info { "Vedtaksforslag med referanse ${request.unikReferanse} finnes allerede. Oppdaterer vedtaksforslaget" }
                val resultat = e.getResponseBodyAs(OpprettVedtakConflictResponse::class.java)!!
                oppdaterVedtaksforslag(resultat.vedtaksid, request)
            } else if (e.statusCode == HttpStatus.PRECONDITION_FAILED) {
                LOGGER.error(e) {
                    "Precondition failed (HTTP 412) ved oppretting av vedtaksforslag " +
                        "med referanse ${request.unikReferanse} " +
                        "for barn ${request.stønadsendringListe.first().kravhaver} " +
                        "i sak ${request.stønadsendringListe.first().sak}"
                }
                throw e
            } else {
                LOGGER.error(e) { "Feil ved oppretting av vedtaksforslag med referanse ${request.unikReferanse}" }
                throw e
            }
        }

    private fun slettEksisterendeVedtaksforslag(referanse: String) {
        hentVedtaksforslagBasertPåReferanase(referanse)?.let {
            LOGGER.info {
                "Fant eksisterende vedtaksforslag med referanse $referanse og id ${it.vedtaksid}. Sletter eksisterende vedtaksforslag "
            }
            slettVedtaksforslag(it.vedtaksid)
        }
    }

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
    override fun hentVedtak(vedtaksid: Int): VedtakDto? =
        getForEntity(
            bidragVedtakUri
                .pathSegment("vedtak")
                .pathSegment(vedtaksid.toString())
                .build()
                .toUri(),
        )

    override fun hentManuelleVedtak(hentManuelleVedtak: HentManuelleVedtakRequest): HentVedtakForStønadResponse {
        TODO("Not yet implemented")
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentVedtakForStønad(hentVedtakForStønadRequest: HentVedtakForStønadRequest): HentVedtakForStønadResponse =
        postForNonNullEntity(
            bidragVedtakUri
                .pathSegment("vedtak")
                .pathSegment("hent-vedtak")
                .build()
                .toUri(),
            hentVedtakForStønadRequest,
        )
}
