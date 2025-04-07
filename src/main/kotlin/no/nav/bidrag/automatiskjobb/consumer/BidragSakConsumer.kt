package no.nav.bidrag.automatiskjobb.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.configuration.CacheConfiguration.Companion.SAKER_PERSON_CACHE
import no.nav.bidrag.automatiskjobb.configuration.CacheConfiguration.Companion.SAK_CACHE
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningSakConsumer
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.sak.BidragssakDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val LOGGER = KotlinLogging.logger {}

@Service
class BidragSakConsumer(
    @Value("\${BIDRAG_SAK_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-sak"),
    BeregningSakConsumer {
    private fun createUri(path: String?) =
        UriComponentsBuilder
            .fromUri(url)
            .path(path ?: "")
            .build()
            .toUri()

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    @Cacheable(SAK_CACHE)
    override fun hentSak(saksnr: String): BidragssakDto {
        try {
            return getForNonNullEntity(createUri("/bidrag-sak/sak/$saksnr"))
        } catch (e: HttpStatusCodeException) {
            LOGGER.warn(e) { "Det skjedde en feil ved henting av sak $saksnr" }
            throw e
        }
    }

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    @Cacheable(SAKER_PERSON_CACHE)
    fun hentSakerForPerson(personIdent: Personident): List<BidragssakDto> {
        try {
            return postForNonNullEntity(createUri("/person/sak"), personIdent)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                LOGGER.warn(e) { "Fant ingen saker for ${personIdent.verdi}" }
                secureLogger.warn("Fant ingen saker for ${personIdent.verdi}")
                return emptyList()
            }
            LOGGER.warn(e) { "Det skjedde en feil ved henting av saker for $personIdent" }
            throw e
        }
    }
}
