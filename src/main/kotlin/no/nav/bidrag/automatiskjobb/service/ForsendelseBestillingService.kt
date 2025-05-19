package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragDokumentForsendelseConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.dokument.forsendelse.MottakerIdentTypeTo
import no.nav.bidrag.transport.dokument.forsendelse.MottakerTo
import no.nav.bidrag.transport.dokument.numeric
import no.nav.bidrag.transport.sak.BidragssakDto
import org.springframework.stereotype.Service
import java.sql.Timestamp

private const val DOKUMENTMAL_BI01B05 = "BI01B05"
private val log = KotlinLogging.logger {}

@Service
class ForsendelseBestillingService(
    private val forsendelseBestillingRepository: ForsendelseBestillingRepository,
    private val bidragDokumentForsendelseConsumer: BidragDokumentForsendelseConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
    private val bidragPersonConsumer: BidragPersonConsumer,
) {
    fun opprettForsendelseBestilling(
        aldersjustering: Aldersjustering,
        gjelderIdent: String,
        mottakerident: String,
        rolletype: Rolletype,
    ) {
        val forsendelseBestilling =
            ForsendelseBestilling(
                aldersjustering = aldersjustering,
                rolletype = rolletype,
                gjelder = gjelderIdent,
                mottaker = mottakerident,
                dokumentmal = DOKUMENTMAL_BI01B05,
                språkkode = Språk.NB,
            )
        val opprettetForsendelse = forsendelseBestillingRepository.save(forsendelseBestilling)
        log.info {
            "Opprettet forsendelse bestilling ${opprettetForsendelse.id} " +
                "for rolle ${opprettetForsendelse.rolletype} med dokumentmalId ${opprettetForsendelse.dokumentmal}" +
                "relatert til aldersjustering ${opprettetForsendelse.aldersjustering.id} og sak ${opprettetForsendelse.aldersjustering.barn.saksnummer}"
        }
    }

    fun slettForsendelse(forsendelseBestilling: ForsendelseBestilling) {
        // Ignorer behandling hvis det er distribuert
        if (forsendelseBestilling.distribuertTidspunkt != null) return

        bidragDokumentForsendelseConsumer.slettForsendelse(
            forsendelseBestilling.forsendelseId!!,
            forsendelseBestilling.aldersjustering.barn.saksnummer,
        )
        log.info {
            "Slettet forsendelse ${forsendelseBestilling.forsendelseId} opprettet ${forsendelseBestilling.forsendelseOpprettetTidspunkt}" +
                "relatert til aldersjustering ${forsendelseBestilling.aldersjustering.id} og sak ${forsendelseBestilling.aldersjustering.barn.saksnummer}"
        }
        forsendelseBestilling.slettetTidspunkt = Timestamp(System.currentTimeMillis())
    }

    fun opprettForsendelse(forsendelseBestilling: ForsendelseBestilling) {
        if (forsendelseBestilling.forsendelseId != null && forsendelseBestilling.skalSlettes) {
            slettForsendelse(forsendelseBestilling)
            val nyForsendelseBestilling =
                ForsendelseBestilling(
                    aldersjustering = forsendelseBestilling.aldersjustering,
                    mottaker = forsendelseBestilling.mottaker,
                    rolletype = forsendelseBestilling.rolletype,
                    språkkode = forsendelseBestilling.språkkode,
                    dokumentmal = forsendelseBestilling.dokumentmal,
                )
            opprettForsendelseTilBidragDokument(nyForsendelseBestilling)
        } else {
            opprettForsendelseTilBidragDokument(forsendelseBestilling)
        }
    }

    fun distribuerForsendelse(forsendelseBestilling: ForsendelseBestilling) {
        val distribuerJournalpostResponse =
            bidragDokumentForsendelseConsumer.distribuerForsendelse(
                forsendelseBestilling.aldersjustering.batchId,
                forsendelseBestilling.forsendelseId!!,
            )
        log.info {
            "Distribuerte forsendelse ${forsendelseBestilling.forsendelseId} " +
                "med journalpostId ${distribuerJournalpostResponse.journalpostId.numeric} " +
                "relatert til aldersjustering ${forsendelseBestilling.aldersjustering.id} og sak ${forsendelseBestilling.aldersjustering.barn.saksnummer}"
        }
        forsendelseBestilling.journalpostId = distribuerJournalpostResponse.journalpostId.numeric
        forsendelseBestilling.distribuertTidspunkt = Timestamp(System.currentTimeMillis())
    }

    private fun opprettForsendelseTilBidragDokument(forsendelseBestilling: ForsendelseBestilling) {
        val mottaker =
            MottakerTo(
                ident = forsendelseBestilling.mottaker,
                språk = forsendelseBestilling.språkkode?.name,
                identType = MottakerIdentTypeTo.FNR,
            )

        if (forsendelseBestilling.gjelder.isNullOrEmpty() || forsendelseBestilling.mottaker.isNullOrEmpty()) {
            log.info { "Forsendelse ${forsendelseBestilling.id} har ingen gjelder eller mottaker. Ignorerer forespørsel" }
            forsendelseBestilling.feilBegrunnelse = "ForsendelseBestilling har ingen gjelder eller mottaker"
            return
        }
        try {
            val forsendelseId =
                bidragDokumentForsendelseConsumer.opprettForsendelse(
                    forsendelseBestilling.aldersjustering,
                    forsendelseBestilling.gjelder,
                    mottaker,
                    forsendelseBestilling.aldersjustering.barn.saksnummer,
                    finnEierfogd(forsendelseBestilling.aldersjustering.barn.saksnummer),
                )
            forsendelseBestilling.forsendelseId = forsendelseId
            forsendelseBestilling.forsendelseOpprettetTidspunkt = Timestamp(System.currentTimeMillis())
            forsendelseBestilling.feilBegrunnelse = null
            log.info {
                "Opprettet forsendelse ${forsendelseBestilling.forsendelseId} for rolle ${forsendelseBestilling.rolletype} " +
                    "relatert til aldersjustering ${forsendelseBestilling.aldersjustering.id} og sak ${forsendelseBestilling.aldersjustering.barn.saksnummer}"
            }
        } catch (e: Exception) {
            log.error(e) {
                "Feil ved opprettelse av forsendelse ${forsendelseBestilling.id} " +
                    "for rolle ${forsendelseBestilling.rolletype} " +
                    "relatert til aldersjustering ${forsendelseBestilling.aldersjustering.id} " +
                    "og sak ${forsendelseBestilling.aldersjustering.barn.saksnummer}"
            }
            forsendelseBestilling.feilBegrunnelse = e.message
        }

        forsendelseBestillingRepository.save(forsendelseBestilling)
    }

    fun opprettBestillingForAldersjustering(aldersjustering: Aldersjustering) {
        val sak = bidragSakConsumer.hentSak(aldersjustering.barn.saksnummer)
        opprettBestillingForBidragspliktig(aldersjustering, sak)
        opprettBestillingForBidragsmottaker(aldersjustering, sak)
    }

    private fun opprettBestillingForBidragspliktig(
        aldersjustering: Aldersjustering,
        sak: BidragssakDto,
    ) {
        val bp = sak.roller.find { it.type == Rolletype.BIDRAGSPLIKTIG }!!
        opprettForsendelseBestilling(
            aldersjustering = aldersjustering,
            gjelderIdent = bp.fødselsnummer!!.verdi,
            mottakerident = bp.fødselsnummer!!.verdi,
            rolletype = Rolletype.BIDRAGSPLIKTIG,
        )
    }

    private fun opprettBestillingForBidragsmottaker(
        aldersjustering: Aldersjustering,
        sak: BidragssakDto,
    ) {
        val bm = sak.roller.find { it.type == Rolletype.BIDRAGSMOTTAKER }!!
        if (erPersonDød(bm.fødselsnummer!!)) {
            log.warn { "Bidragsmottaker er død. Oppretter ikke forsendelse. Gjelder aldersjustering ${aldersjustering.id}" }
            return
        }
        opprettForsendelseBestilling(
            aldersjustering = aldersjustering,
            gjelderIdent = bm.fødselsnummer!!.verdi,
            mottakerident = bm.fødselsnummer!!.verdi,
            rolletype = Rolletype.BIDRAGSMOTTAKER,
        )
    }

    private fun erPersonDød(personident: Personident): Boolean {
        val person = bidragPersonConsumer.hentPerson(personident)
        return person.dødsdato != null
    }

    private fun finnEierfogd(saksnummer: String): String {
        val sak = bidragSakConsumer.hentSak(saksnummer)
        return sak.eierfogd.verdi
    }
}
