package no.nav.bidrag.automatiskjobb.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.dto.OpprettBehandlingRequest
import no.nav.bidrag.automatiskjobb.consumer.dto.OpprettBehandlingResponse
import no.nav.bidrag.automatiskjobb.consumer.dto.OpprettRolleDto
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
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
import java.time.LocalDate

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

    fun opprettBehandlingForRevurderingAvForskudd(
        revurderingForskudd: RevurderingForskudd,
        bm: Personident?,
    ): OpprettBehandlingResponse {
        val opprettBehandlingRequest =
            OpprettBehandlingRequest(
                behandlingstema = Behandlingstema.FORSKUDD,
                vedtakstype = Vedtakstype.REVURDERING,
                søktFomDato = LocalDate.now(), // TODO(Hva skal denne være?)
                mottattdato = LocalDate.now(), // TODO(Hva skal denne være?)
                søknadFra = SøktAvType.NAV_BIDRAG,
                saksnummer = revurderingForskudd.barn.saksnummer,
                behandlerenhet = "0001", // TODO(Hvilken enhet skal settes her?)
                roller =
                    setOf(
                        OpprettRolleDto(
                            rolletype = Rolletype.BIDRAGSMOTTAKER,
                            ident = bm,
                            fødselsdato = null,
                            harGebyrsøknad = false, // TODO(Hva skal det være her?)
                        ),
                        OpprettRolleDto(
                            rolletype = Rolletype.BARN,
                            ident = Personident(revurderingForskudd.barn.kravhaver),
                            fødselsdato = null,
                            harGebyrsøknad = false, // TODO(Hva skal det være her?)
                        ),
                    ),
                stønadstype = Stønadstype.FORSKUDD,
                søknadsid = null,
                opprettSøknad = true,
            )
        return try {
            postForNonNullEntity(
                createUri("/api/v2/behandling"),
                opprettBehandlingRequest,
            )
        } catch (
            e: HttpStatusCodeException,
        ) {
            LOGGER.error(e) { "Det skjedde en feil ved opprettelse av revurderingslenke for barn ${revurderingForskudd.barn}" }
            throw e
        }
    }
}
