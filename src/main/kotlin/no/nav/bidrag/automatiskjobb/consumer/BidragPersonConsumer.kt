package no.nav.bidrag.automatiskjobb.consumer

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
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

data class HusstandsmedlemmerRequest(
    val personRequest: PersonRequest,
    val periodeFra: LocalDate?,
)

@Service
class BidragPersonConsumer(
    @Value("\${PERSON_URL}") private val bidragPersonUrl: URI,
    @Qualifier("azure") private val restTemplate: RestTemplate,
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
    override fun hentFødselsdatoForPerson(personident: Personident): LocalDate {
        val response = postForNonNullEntity<NavnFødselDødDto>(hentFødselsnummerUri, PersonRequest(personident))
        return response.fødselsdato ?: opprettFødselsdatoFraFødselsår(response.fødselsår!!)
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
