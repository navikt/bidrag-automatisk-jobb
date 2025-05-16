package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragDokumentForsendelseConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.transport.dokument.forsendelse.MottakerIdentTypeTo
import no.nav.bidrag.transport.dokument.forsendelse.MottakerTo
import no.nav.bidrag.transport.dokument.numeric
import org.springframework.stereotype.Service
import java.sql.Timestamp

private const val DOKUMENTMAL_BI01B05 = "BI01B05"
private val log = KotlinLogging.logger {}

@Service
class ForsendelseBestillingService(
    private val forsendelseBestillingRepository: ForsendelseBestillingRepository,
    private val bidragDokumentForsendelseConsumer: BidragDokumentForsendelseConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
) {
    fun opprettForsendelseBestilling(
        aldersjustering: Aldersjustering,
        mottakerident: String,
        rolletype: Rolletype,
    ) {
        val forsendelseBestilling =
            ForsendelseBestilling(
                aldersjustering = aldersjustering,
                rolletype = rolletype,
                mottaker = mottakerident,
                dokumentmal = DOKUMENTMAL_BI01B05,
                språkkode = Språk.NB,
            )
        forsendelseBestillingRepository.save(forsendelseBestilling)
    }

    fun slettForsendelse(forsendelseBestilling: ForsendelseBestilling) {
        // Ignorer behandling hvis det er distribuert
        if (forsendelseBestilling.distribuertTidspunkt != null) return

        log.info {
            "Sletter forsendelse ${forsendelseBestilling.forsendelseId} opprettet ${forsendelseBestilling.forsendelseOpprettetTidspunkt}"
        }

        bidragDokumentForsendelseConsumer.slettForsendelse(
            forsendelseBestilling.forsendelseId!!,
            forsendelseBestilling.aldersjustering.barn.saksnummer,
        )
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

        val forsendelseId =
            bidragDokumentForsendelseConsumer.opprettForsendelse(
                forsendelseBestilling.aldersjustering,
                mottaker,
                forsendelseBestilling.aldersjustering.barn.saksnummer,
                finnEierfogd(forsendelseBestilling.aldersjustering.barn.saksnummer),
            )

        forsendelseBestilling.forsendelseId = forsendelseId
        forsendelseBestilling.forsendelseOpprettetTidspunkt = Timestamp(System.currentTimeMillis())
        log.info {
            "Opprettet forsendelse ${forsendelseBestilling.forsendelseId} for rolle ${forsendelseBestilling.rolletype} relatert til aldersjustering ${forsendelseBestilling.aldersjustering.id}"
        }
        forsendelseBestillingRepository.save(forsendelseBestilling)
    }

    private fun finnEierfogd(saksnummer: String): String {
        val sak = bidragSakConsumer.hentSak(saksnummer)
        return sak.eierfogd.verdi
    }
}
