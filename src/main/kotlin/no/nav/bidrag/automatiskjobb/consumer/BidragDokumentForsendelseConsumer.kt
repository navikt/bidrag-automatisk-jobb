package no.nav.bidrag.automatiskjobb.consumer

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.DistribuerJournalpostRequest
import no.nav.bidrag.transport.dokument.DistribuerJournalpostResponse
import no.nav.bidrag.transport.dokument.forsendelse.BehandlingInfoDto
import no.nav.bidrag.transport.dokument.forsendelse.JournalTema
import no.nav.bidrag.transport.dokument.forsendelse.MottakerTo
import no.nav.bidrag.transport.dokument.forsendelse.OpprettDokumentForespørsel
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseForespørsel
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseRespons
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

val dokumentMaler =
    mapOf(
        Stønadstype.BIDRAG to "BI01B05",
    )

@Service
class BidragDokumentForsendelseConsumer(
    @Value("\${BIDRAG_DOKUMENT_FORSENDELSE_URL}") private val url: URI,
    @Qualifier("azure") restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-dokument-forsendelse") {
    private fun createUri(path: String = "") = URI.create("$url/$path")

    fun opprettForsendelse(
        aldersjustering: Aldersjustering,
        mottakerTo: MottakerTo,
        saksnummer: String,
        enhet: String,
    ): Long? {
        val opprettForsendelseForespørsel =
            OpprettForsendelseForespørsel(
                gjelderIdent = mottakerTo.ident!!,
                mottaker = mottakerTo,
                saksnummer = saksnummer,
                enhet = enhet,
                batchId = aldersjustering.batchId,
                tema = JournalTema.BID,
                behandlingInfo =
                    BehandlingInfoDto(
                        vedtakId = aldersjustering.vedtak.toString(),
                        stonadType = aldersjustering.stønadstype,
                        barnIBehandling = listOf(aldersjustering.barn.kravhaver),
                        erFattetBeregnet = true,
                        soknadType = "EGET_TILTAK",
                        soknadFra = SøktAvType.NAV_BIDRAG,
                        vedtakType = Vedtakstype.ALDERSJUSTERING,
                    ),
                dokumenter =
                    listOf(
                        OpprettDokumentForespørsel(
                            dokumentmalId = dokumentMaler[aldersjustering.stønadstype],
                            bestillDokument = true,
                        ),
                    ),
            )

        val forsendelseResponse =
            postForNonNullEntity<OpprettForsendelseRespons>(
                createUri("api/forsendelse"),
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
        postForEntity<Any>(
            createUri("api/forsendelse/journal/BIF-$forsendelseId/avvik"),
            avviksHendelse,
        )
    }

    fun distribuerForsendelse(
        batchId: String,
        forsendelseId: Long,
    ): DistribuerJournalpostResponse {
        val distribuerJournalpostRequest = DistribuerJournalpostRequest(batchId = batchId)

        return postForNonNullEntity<DistribuerJournalpostResponse>(
            createUri("api/forsendelse/journal/distribuer/$forsendelseId?batchId=$batchId"),
            distribuerJournalpostRequest,
        )
    }
}
