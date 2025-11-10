package no.nav.bidrag.automatiskjobb.mapper

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSamhandlerConsumer
import no.nav.bidrag.automatiskjobb.consumer.dokumentMaler
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.transport.dokument.forsendelse.BehandlingInfoDto
import no.nav.bidrag.transport.dokument.forsendelse.JournalTema
import no.nav.bidrag.transport.dokument.forsendelse.MottakerIdentTypeTo
import no.nav.bidrag.transport.dokument.forsendelse.MottakerTo
import no.nav.bidrag.transport.dokument.forsendelse.OpprettDokumentForespørsel
import no.nav.bidrag.transport.dokument.forsendelse.OpprettForsendelseForespørsel
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class ForsendelseMapper(
    private val bidragSakConsumer: BidragSakConsumer,
    private val samhandlerConsumer: BidragSamhandlerConsumer,
) {
    fun tilOpprettForsendelseRequest(forsendelseBestilling: ForsendelseBestilling): OpprettForsendelseForespørsel? {
        val mottakerErSamhandler = forsendelseBestilling.mottaker?.let { SamhandlerId(it).gyldig() } == true
        val navn = if (mottakerErSamhandler) samhandlerConsumer.hentSamhandler(forsendelseBestilling.mottaker!!)?.navn else null
        val mottaker =
            MottakerTo(
                ident = forsendelseBestilling.mottaker,
                språk = forsendelseBestilling.språkkode?.name,
                navn = navn,
                identType =
                    if (mottakerErSamhandler) {
                        MottakerIdentTypeTo.SAMHANDLER
                    } else {
                        MottakerIdentTypeTo.FNR
                    },
            )

        if (forsendelseBestilling.gjelder.isNullOrEmpty() || forsendelseBestilling.mottaker.isNullOrEmpty()) {
            log.info { "Forsendelse ${forsendelseBestilling.id} har ingen gjelder eller mottaker. Ignorerer forespørsel" }
            forsendelseBestilling.feilBegrunnelse = "ForsendelseBestilling mangler gjelder eller/og mottaker"
            return null
        }

        val unikReferanse = forsendelseBestilling.unikReferanse
        val vedtak = forsendelseBestilling.vedtak
        val stonadType = forsendelseBestilling.stønadstype
        val kravhaver = forsendelseBestilling.barn.kravhaver
        val batchId = forsendelseBestilling.batchId
        val saksnummer = forsendelseBestilling.barn.saksnummer
        val enhet = finnEierfogd(forsendelseBestilling.barn.saksnummer)

        return OpprettForsendelseForespørsel(
            unikReferanse = unikReferanse,
            gjelderIdent = forsendelseBestilling.gjelder,
            mottaker = mottaker,
            saksnummer = saksnummer,
            enhet = enhet,
            batchId = batchId,
            tema = JournalTema.BID,
            behandlingInfo =
                BehandlingInfoDto(
                    vedtakId = vedtak.toString(),
                    stonadType = stonadType,
                    barnIBehandling = listOf(kravhaver),
                    erFattetBeregnet = true,
                    soknadType = "EGET_TILTAK",
                    soknadFra = SøktAvType.NAV_BIDRAG,
                    vedtakType = Vedtakstype.ALDERSJUSTERING,
                ),
            dokumenter =
                listOf(
                    OpprettDokumentForespørsel(
                        dokumentmalId = dokumentMaler[stonadType],
                        bestillDokument = true,
                    ),
                ),
        )
    }

    private fun finnEierfogd(saksnummer: String): String {
        val sak = bidragSakConsumer.hentSak(saksnummer)
        return sak.eierfogd.verdi
    }
}
