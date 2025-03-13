package no.nav.bidrag.automatiskjobb.consumer

import no.nav.bidrag.automatiskjobb.configuration.CacheConfiguration.Companion.PERSON_CACHE
import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class BidragPersonConsumer(
    @Value("\${PERSON_URL}") bidragPersonUrl: URI,
    @Qualifier("azure") private val restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-automatisk-jobb") {
    private val hentFødselsnummerUri =
        UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment("navnfoedseldoed")
            .build()
            .toUri()

    @BrukerCacheable(PERSON_CACHE)
    fun hentFødselsdatoForPerson(personident: Personident): LocalDate {
        val response = postForNonNullEntity<NavnFødselDødDto>(hentFødselsnummerUri, PersonRequest(personident))
        return response.fødselsdato ?: opprettFødselsdatoFraFødselsår(response.fødselsår)
    }

    private fun opprettFødselsdatoFraFødselsår(fødselsår: Int): LocalDate {
        // Fødselsår finnes for alle i PDL, mens noen ikke har utfyllt fødselsdato. I disse tilfellene settes 1. januar.
        return LocalDate.of(fødselsår, 1, 1)
    }
}
