package no.nav.bidrag.automatiskjobb.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.configuration.CacheConfiguration.Companion.PERSON_CACHE
import no.nav.bidrag.automatiskjobb.configuration.CacheConfiguration.Companion.PERSON_FØDSELSDATO_CACHE
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningPersonConsumer
import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.HusstandsmedlemmerDto
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

private val log = KotlinLogging.logger {}

data class HusstandsmedlemmerRequest(
    val personRequest: PersonRequest,
    val periodeFra: LocalDate?,
)

@Service
class BidragPersonConsumer(
    @param:Value("\${PERSON_URL}") private val bidragPersonUrl: URI,
    @param:Qualifier("azure") private val restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-automatisk-jobb"),
    BeregningPersonConsumer {
    private val hentFødselsnummerUri =
        UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment("navnfoedseldoed")
            .build()
            .toUri()

    private fun createUri(path: String?) =
        UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment(path ?: "")
            .build()
            .toUri()

    @BrukerCacheable(PERSON_FØDSELSDATO_CACHE)
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentFødselsdatoForPerson(kravhaver: Personident): LocalDate? {
        try {
            val response = postForNonNullEntity<NavnFødselDødDto>(hentFødselsnummerUri, PersonRequest(kravhaver))
            return response.fødselsdato ?: response.fødselsår?.let { opprettFødselsdatoFraFødselsår(it) }
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode.value() == HttpStatus.NOT_FOUND.value()) {
                return null
            }
            throw e
        }
    }

    @BrukerCacheable(PERSON_CACHE)
    fun hentPerson(personident: Personident): PersonDto =
        postForNonNullEntity<PersonDto>(createUri("informasjon"), PersonRequest(personident))

    fun hentPersonHusstandsmedlemmer(personident: Personident): HusstandsmedlemmerDto =
        postForNonNullEntity<HusstandsmedlemmerDto>(
            createUri("husstandsmedlemmer"),
            HusstandsmedlemmerRequest(PersonRequest(personident), LocalDate.now()),
        )

    private fun opprettFødselsdatoFraFødselsår(fødselsår: Int): LocalDate {
        // Fødselsår finnes for alle i PDL, mens noen ikke har utfyllt fødselsdato. I disse tilfellene settes 1. januar.
        return LocalDate.of(fødselsår, 1, 1)
    }
}
