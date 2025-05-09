package no.nav.bidrag.automatiskjobb.consumer

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.forsendelse.JournalTema
import no.nav.bidrag.transport.dokument.forsendelse.MottakerTo
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseForespørsel
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseRespons
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class BidragDokumentForsendelseConsumer(
    @Value("\${BIDRAG_DOKUMENT_FORSENDELSE_URL}") private val url: URI,
    @Qualifier("azure") restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-dokument-forsendelse") {
    private fun createUri(path: String = "") = URI.create("$url/$path")

    fun sendForsendelse(
        aldersjustering: Aldersjustering,
        mottakerTo: MottakerTo,
        saksnummer: String,
        enhet: String,
    ): Long? {
        val opprettForsendelseForespørsel =
            OpprettForsendelseForespørsel(
                gjelderIdent = aldersjustering.barn.kravhaver,
                mottaker = mottakerTo,
                saksnummer = saksnummer,
                enhet = enhet,
                batchId = aldersjustering.batchId,
                tema = JournalTema.BID,
            )

        val forsendelseResponse =
            postForNonNullEntity<OpprettForsendelseRespons>(
                createUri("/api/forsendelse"),
                opprettForsendelseForespørsel,
            )
        return forsendelseResponse.forsendelseId
    }

    fun slettForsendelse(
        forsendelseId: Long,
        saksnummer: String,
    ) {
        val avviksHendelse =
            Avvikshendelse(
                avvikType = "SLETT_JOURNALPOST",
                saksnummer = saksnummer,
            )
        postForNonNullEntity<Any>(
            createUri("/api/forsendelse/journal/BIF_$forsendelseId/avvik"),
            avviksHendelse,
        )
    }
}
