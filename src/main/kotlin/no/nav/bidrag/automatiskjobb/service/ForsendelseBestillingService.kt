package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragDokumentForsendelseConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSamhandlerConsumer
import no.nav.bidrag.automatiskjobb.mapper.ForsendelseMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import no.nav.bidrag.automatiskjobb.utils.bidragsmottaker
import no.nav.bidrag.automatiskjobb.utils.bidragspliktig
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.dokument.numeric
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.RolleDto
import no.nav.bidrag.transport.samhandler.Områdekode
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.LocalDate

private const val DOKUMENTMAL_BI01B05 = "BI01B05"
private val log = KotlinLogging.logger {}

data class ForsendelseGjelderMottakerInfo(
    val gjelder: String?,
    val mottakerIdent: String?,
    val mottakerRolle: Rolletype,
    val feilBegrunnelse: String? = null,
)

@Service
class ForsendelseBestillingService(
    private val forsendelseBestillingRepository: ForsendelseBestillingRepository,
    private val bidragDokumentForsendelseConsumer: BidragDokumentForsendelseConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
    private val bidragPersonConsumer: BidragPersonConsumer,
    private val samhandlerConsumer: BidragSamhandlerConsumer,
    private val mapper: ForsendelseMapper,
) {
    fun opprettForsendelseBestilling(
        aldersjustering: Aldersjustering,
        gjelderIdent: String?,
        mottakerident: String?,
        rolletype: Rolletype,
        feilBegrunnelse: String? = null,
    ) {
        val forsendelseBestilling =
            ForsendelseBestilling(
                aldersjustering = aldersjustering,
                rolletype = rolletype,
                gjelder = gjelderIdent,
                mottaker = mottakerident,
                dokumentmal = DOKUMENTMAL_BI01B05,
                språkkode = Språk.NB,
                feilBegrunnelse = feilBegrunnelse,
            )
        val opprettetForsendelse = forsendelseBestillingRepository.save(forsendelseBestilling)
        log.info {
            "Opprettet forsendelse bestilling ${opprettetForsendelse.id} " +
                "for rolle ${opprettetForsendelse.rolletype} med dokumentmalId ${opprettetForsendelse.dokumentmal} " +
                "relatert til aldersjustering ${opprettetForsendelse.aldersjustering.id} og sak ${opprettetForsendelse.aldersjustering.barn.saksnummer}"
        }
    }

    fun slettForsendelse(forsendelseBestilling: ForsendelseBestilling) {
        // Ignorer behandling hvis det er distribuert
        if (forsendelseBestilling.distribuertTidspunkt != null || forsendelseBestilling.slettetTidspunkt != null) return

        if (forsendelseBestilling.forsendelseId == null) {
            forsendelseBestillingRepository.delete(forsendelseBestilling)
            return
        }

        bidragDokumentForsendelseConsumer.slettForsendelse(
            forsendelseBestilling.forsendelseId!!,
            forsendelseBestilling.aldersjustering.barn.saksnummer,
        )
        log.info {
            "Slettet forsendelse ${forsendelseBestilling.forsendelseId} opprettet ${forsendelseBestilling.forsendelseOpprettetTidspunkt} " +
                "relatert til aldersjustering ${forsendelseBestilling.aldersjustering.id} og sak ${forsendelseBestilling.aldersjustering.barn.saksnummer}"
        }
        forsendelseBestilling.slettetTidspunkt = Timestamp(System.currentTimeMillis())
    }

    fun opprettForsendelse(
        forsendelseBestilling: ForsendelseBestilling,
        prosesserFeilet: Boolean,
    ) {
        if (!forsendelseBestilling.feilBegrunnelse.isNullOrEmpty() && !prosesserFeilet) {
            log.warn {
                "Forsendelse bestilling ${forsendelseBestilling.id} har feilet med begrunnelse ${forsendelseBestilling.feilBegrunnelse}. " +
                    "Ignorer bestilling"
            }
            return
        }
        if (forsendelseBestilling.forsendelseId != null && forsendelseBestilling.skalSlettes) {
            slettForsendelse(forsendelseBestilling)
            val nyForsendelseBestilling =
                ForsendelseBestilling(
                    aldersjustering = forsendelseBestilling.aldersjustering,
                    gjelder = forsendelseBestilling.gjelder,
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
        val request = mapper.tilOpprettForsendelseRequest(forsendelseBestilling) ?: return
        try {
            val forsendelseId =
                bidragDokumentForsendelseConsumer.opprettForsendelse(request)
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
        if (sak.bidragspliktig == null || erPersonDød(sak.bidragspliktig!!.fødselsnummer!!)) {
            log.warn {
                "Bidragspliktig finnes ikke eller er død. Oppretter ikke forsendelse. Gjelder aldersjustering ${aldersjustering.id}"
            }
            return
        }
        slettEksisterendeBestillingerSomIkkeErOpprettet(aldersjustering)
        opprettBestillingForBidragspliktig(aldersjustering, sak)
        opprettBestillingForBidragsmottaker(aldersjustering, sak)
    }

    private fun slettEksisterendeBestillingerSomIkkeErOpprettet(aldersjustering: Aldersjustering) {
        val eksisterende = forsendelseBestillingRepository.findByAldersjustering(aldersjustering)
        eksisterende
            .filter {
                it.skalSlettes && it.slettetTidspunkt == null || it.forsendelseId == null && it.distribuertTidspunkt == null
            }.forEach {
                slettForsendelse(it)
            }
    }

    private fun opprettBestillingForBidragspliktig(
        aldersjustering: Aldersjustering,
        sak: BidragssakDto,
    ) {
        val bp = sak.bidragspliktig!!
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
        val mottakerIdent = finnMottakerIdent(aldersjustering, sak)
        opprettForsendelseBestilling(
            aldersjustering = aldersjustering,
            gjelderIdent = mottakerIdent.gjelder,
            mottakerident = mottakerIdent.mottakerIdent,
            rolletype = mottakerIdent.mottakerRolle,
            feilBegrunnelse = mottakerIdent.feilBegrunnelse,
        )
    }

    private fun erPersonDød(personident: Personident): Boolean {
        val person = bidragPersonConsumer.hentPerson(personident)
        return person.dødsdato != null
    }

    private fun finnMottakerIdent(
        aldersjustering: Aldersjustering,
        sak: BidragssakDto,
    ): ForsendelseGjelderMottakerInfo {
        val rolleBarn = sak.roller.find { it.fødselsnummer?.verdi == aldersjustering.barn.kravhaver }!!

        val bm =
            sak.bidragsmottaker ?: return ForsendelseGjelderMottakerInfo(
                null,
                null,
                Rolletype.BIDRAGSMOTTAKER,
                feilBegrunnelse = "Sak ${sak.saksnummer} har ingen bidragsmottaker",
            )

        val mottakerGjelderBM = ForsendelseGjelderMottakerInfo(bm.fødselsnummer?.verdi, bm.fødselsnummer?.verdi, Rolletype.BIDRAGSMOTTAKER)
        if (rolleBarn.reellMottaker != null) {
            return finnRmSomMottaker(rolleBarn) ?: mottakerGjelderBM
        }
        return mottakerGjelderBM
    }

    private fun finnRmSomMottaker(barn: RolleDto): ForsendelseGjelderMottakerInfo? {
        // *** REGEL ***
        // hvis RM er lik barn så betyr det at barnet bor for seg selv. Sendt forsendelse til barnet hvis barnet er over 18
        // hvis RM er institusjon
        // hvis institusjon er VERGE -> forsendelse
        // hvis IKKE VERGE -> sjekk om institusjon er barnevern institusjon
        // -> hvis barnevern inst -> forsendelse
        // -> hvis IKKE barnevern inst -> forsendelse til BM

        val rm = barn.reellMottaker ?: return null
        if (rm.ident.verdi == barn.fødselsnummer!!.verdi) {
            return if (erOver18År(barn.fødselsnummer!!)) {
                secureLogger.info {
                    "Fødselsnummer til RM til barn er har samme fødselsnummer som barnet ${barn.fødselsnummer!!.verdi} og barnet er over 18 år. Setter mottaker av forsendelsen til barnet"
                }
                ForsendelseGjelderMottakerInfo(
                    barn.fødselsnummer!!.verdi,
                    barn.fødselsnummer!!.verdi,
                    Rolletype.BARN,
                )
            } else {
                secureLogger.info {
                    "Fødselsnummer til RM til barn er har samme fødselsnummer som barnet ${barn.fødselsnummer!!.verdi} og men er under 18 år. Setter mottaker av forsendelsen til BM"
                }
                null
            }
        }
        // sjekker om denne reelle mottaker er verge -> forsendelse skal til RM
        if (rm.verge) {
            secureLogger.info { "RM ${rm.ident} til barn ${barn.fødselsnummer} er verge. Setter mottaker av forsendelsen til RM" }
            // Hvis reell mottaker eksisterer og er verge, sendes forsendelsen til vergen
            return ForsendelseGjelderMottakerInfo(barn.fødselsnummer!!.verdi, rm.ident.verdi, Rolletype.REELMOTTAKER)
        }

        if (!rm.ident.gyldig()) {
            log.warn {
                "RM har ikke en gyldig samhandlerId ${rm.ident}. Går videre uten å sjekke om RM er Barnevernsinstitusjon"
            }
            return null
        }

        val samhandler =
            samhandlerConsumer.hentSamhandler(rm.ident.verdi) ?: return ForsendelseGjelderMottakerInfo(
                barn.fødselsnummer!!.verdi,
                rm.ident.verdi,
                Rolletype.REELMOTTAKER,
                "Fant ikke samhandler med id ${rm.ident} i bidrag-samhandler",
            )
        if (samhandler.områdekode == Områdekode.BARNEVERNSINSTITUSJON) {
            secureLogger.info {
                "RM ${rm.ident} til barnet ${barn.fødselsnummer} er barnevernsinstitusjon." +
                    " Setter mottaker av forsendelsen til RM"
            }
            return ForsendelseGjelderMottakerInfo(
                barn.fødselsnummer!!.verdi,
                rm.ident.verdi,
                Rolletype.REELMOTTAKER,
            )
        }
        secureLogger.info {
            "RM ${rm.ident} til barnet ${barn.fødselsnummer} er ikke barnevernsinstitusjon eller verge." +
                " Setter mottaker av forsendelsen til BM"
        }
        // hvis RM er samhandler, men ikke barnevern institusjon skal brevet sendes til BM
        return null
    }

    private fun erOver18År(personident: Personident): Boolean {
        val fødselsdato = bidragPersonConsumer.hentFødselsdatoForPerson(personident)
        // TODO: Bør dette sjekkes for en eksakt dato (feks 1.Juli?). Ikke behov i aldersjustering pga barnet alltid er under 18
        return fødselsdato != null && fødselsdato.plusYears(18).isBefore(LocalDate.now())
    }
}
