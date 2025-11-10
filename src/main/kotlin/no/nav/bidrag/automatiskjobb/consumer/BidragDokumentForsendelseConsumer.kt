package no.nav.bidrag.automatiskjobb.consumer

import no.nav.bidrag.automatiskjobb.SECURE_LOGGER
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.DistribuerJournalpostRequest
import no.nav.bidrag.transport.dokument.DistribuerJournalpostResponse
import no.nav.bidrag.transport.dokument.forsendelse.ForsendelseConflictResponse
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseForespørsel
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseRespons
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import java.net.URI

val dokumentMaler =
    mapOf(
        Stønadstype.BIDRAG to "BI01B05",
    )

@Service
class BidragDokumentForsendelseConsumer(
    @param:Value($$"${BIDRAG_DOKUMENT_FORSENDELSE_URL}") private val url: URI,
    @Qualifier("azure") restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-dokument-forsendelse") {
    private fun createUri(path: String = "") = URI.create("$url/$path")

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun opprettForsendelse(opprettForsendelseForespørsel: OpprettForsendelseForespørsel): Long? {
        try {
            val forsendelseResponse =
                postForNonNullEntity<OpprettForsendelseRespons>(
                    createUri("api/forsendelse"),
                    opprettForsendelseForespørsel,
                )
            return forsendelseResponse.forsendelseId
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                SECURE_LOGGER.info { "Forsendelse med referanse ${opprettForsendelseForespørsel.unikReferanse} finnes allerede. " }
                val resultat = e.getResponseBodyAs(ForsendelseConflictResponse::class.java)!!
                return resultat.forsendelseId
            } else {
                SECURE_LOGGER.error(e) { "Feil ved oppretting av forsendelse med referanse ${opprettForsendelseForespørsel.unikReferanse}" }
                throw e
            }
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun slettForsendelse(
        forsendelseId: Long,
        saksnummer: String,
    ) {
        val avviksHendelse =
            Avvikshendelse(
                avvikType = "SLETT_JOURNALPOST",
                saksnummer = saksnummer,
            )
        postForEntity<Any>(
            createUri("api/forsendelse/journal/BIF-$forsendelseId/avvik"),
            avviksHendelse,
        )
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun distribuerForsendelse(
        batchId: String,
        forsendelseId: Long,
    ): DistribuerJournalpostResponse {
        val distribuerJournalpostRequest = DistribuerJournalpostRequest(batchId = batchId)

        try {
            return postForNonNullEntity<DistribuerJournalpostResponse>(
                createUri("api/forsendelse/journal/distribuer/$forsendelseId?batchId=$batchId"),
                distribuerJournalpostRequest,
            )
        } catch (e: HttpStatusCodeException) {
            val begrunnelse = e.responseHeaders?.getOrEmpty(HttpHeaders.WARNING)?.firstOrNull()
            throw HttpClientErrorException(e.statusCode, begrunnelse ?: e.message ?: "Ukjent feil ved distribusjon av forsendelse")
        }
    }
}
