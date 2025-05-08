package no.nav.bidrag.automatiskjobb.service

import no.nav.bidrag.automatiskjobb.consumer.BidragDokumentForsendelseConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.transport.dokument.forsendelse.MottakerIdentTypeTo
import no.nav.bidrag.transport.dokument.forsendelse.MottakerTo
import org.springframework.stereotype.Service
import java.sql.Timestamp

private const val DOKUMENTMAL_BI01B05 = "BI01B05"

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

    fun sendForsendelse(forsendelseBestilling: ForsendelseBestilling) {
        if (forsendelseBestilling.forsendelseId != null && forsendelseBestilling.distribuerTidspunkt != null) {
            bidragDokumentForsendelseConsumer.slettForsendelse(
                forsendelseBestilling.forsendelseId!!,
                forsendelseBestilling.aldersjustering.barn.saksnummer,
            )
            forsendelseBestilling.slettetTidspunkt = Timestamp(System.currentTimeMillis())

            val nyForsendelseBestilling =
                ForsendelseBestilling(
                    aldersjustering = forsendelseBestilling.aldersjustering,
                    mottaker = forsendelseBestilling.mottaker,
                    rolletype = forsendelseBestilling.rolletype,
                    språkkode = forsendelseBestilling.språkkode,
                    dokumentmal = forsendelseBestilling.dokumentmal,
                )
            sendForsendelseTilBidragDokument(nyForsendelseBestilling)
        } else {
            sendForsendelseTilBidragDokument(forsendelseBestilling)
        }
    }

    private fun sendForsendelseTilBidragDokument(forsendelseBestilling: ForsendelseBestilling) {
        val mottaker =
            MottakerTo(
                ident = forsendelseBestilling.mottaker,
                språk = forsendelseBestilling.språkkode?.name,
                identType = MottakerIdentTypeTo.FNR,
            )

        val forsendelseId =
            bidragDokumentForsendelseConsumer.sendForsendelse(
                forsendelseBestilling.aldersjustering,
                mottaker,
                forsendelseBestilling.aldersjustering.barn.saksnummer,
                finnEierfogd(forsendelseBestilling.aldersjustering.barn.saksnummer),
            )

        forsendelseBestilling.forsendelseId = forsendelseId
        forsendelseBestilling.bestiltTidspunkt = Timestamp(System.currentTimeMillis())
        forsendelseBestillingRepository.save(forsendelseBestilling)
    }

    private fun finnEierfogd(saksnummer: String): String {
        val sak = bidragSakConsumer.hentSak(saksnummer)
        return sak.eierfogd.verdi
    }
}
