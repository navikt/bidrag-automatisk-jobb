package no.nav.bidrag.automatiskjobb.consumer

import no.nav.bidrag.automatiskjobb.consumer.dto.OppdaterOppgaveDto
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveDto
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveSokRequest
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveSokResponse
import no.nav.bidrag.automatiskjobb.consumer.dto.OpprettOppgaveRequest
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class OppgaveConsumer(
    @Value("\${OPPGAVE_URL}") private val oppgaveUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "oppgave") {
    private val oppgaveURI
        get() = UriComponentsBuilder.fromUri(oppgaveUrl).pathSegment("api", "v1", "oppgaver")

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun opprettOppgave(request: OpprettOppgaveRequest): OppgaveDto =
        postForNonNullEntity(
            oppgaveURI.build().toUri(),
            request,
        )

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun hentOppgave(request: OppgaveSokRequest): OppgaveSokResponse =
        getForNonNullEntity(
            oppgaveURI.query(request.hentParametre()).build().toUri(),
        )

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun hentOppgaveForId(id: Int): OppgaveDto =
        getForNonNullEntity(
            oppgaveURI.pathSegment(id.toString()).build().toUri(),
        )

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun slettOppgave(
        id: Int,
        versjon: Int,
    ): OppgaveDto =
        patchForEntity(
            oppgaveURI.pathSegment(id.toString()).build().toUri(),
            OppdaterOppgaveDto(versjon, "FEILREGISTRERT"),
        )!!
}
