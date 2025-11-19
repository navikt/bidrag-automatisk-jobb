package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragDokumentForsendelseConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSamhandlerConsumer
import no.nav.bidrag.automatiskjobb.mapper.ForsendelseMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseEntity
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Forsendelsestype
import no.nav.bidrag.automatiskjobb.persistence.repository.ForsendelseBestillingRepository
import no.nav.bidrag.automatiskjobb.utils.bidragsmottaker
import no.nav.bidrag.automatiskjobb.utils.bidragspliktig
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.samhandler.Områdekode
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.transport.dokument.numeric
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

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
    fun opprettBestilling(
        forsendelseEntity: ForsendelseEntity,
        forsendelsestype: Forsendelsestype,
    ): MutableList<ForsendelseBestilling> {
        val sak = bidragSakConsumer.hentSak(forsendelseEntity.barn.saksnummer)
        if (sak.bidragspliktig == null || erPersonDød(sak.bidragspliktig!!.fødselsnummer!!)) {
            LOGGER.warn {
                "Bidragspliktig finnes ikke eller er død. Oppretter ikke forsendelse. Gjelder ${forsendelseEntity.id}"
            }
            return mutableListOf()
        }
        slettEksisterendeBestillingerSomIkkeErOpprettet(forsendelseEntity)

        val forsendelseBestillinger = mutableListOf<ForsendelseBestilling>()
        if (forsendelsestype.forsendelseTilBp) {
            forsendelseBestillinger.add(opprettBestillingForBidragspliktig(forsendelseEntity, sak, forsendelsestype))
        }
        if (forsendelsestype.forsendelseTilBm) {
            forsendelseBestillinger.add(opprettBestillingForBidragsmottaker(forsendelseEntity, sak, forsendelsestype))
        }
        return forsendelseBestillinger
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
            forsendelseBestilling.barn.saksnummer,
        )
        LOGGER.info {
            "Slettet forsendelse ${forsendelseBestilling.forsendelseId} opprettet ${forsendelseBestilling.forsendelseOpprettetTidspunkt} " +
                "relatert til sak ${forsendelseBestilling.barn.saksnummer}"
        }
        forsendelseBestilling.slettetTidspunkt = Timestamp(System.currentTimeMillis())
        forsendelseBestillingRepository.save(forsendelseBestilling)
    }

    fun opprettForsendelse(
        forsendelseBestilling: ForsendelseBestilling,
        prosesserFeilet: Boolean,
    ) {
        if (!forsendelseBestilling.feilBegrunnelse.isNullOrEmpty() && !prosesserFeilet) {
            LOGGER.warn {
                "Forsendelse bestilling ${forsendelseBestilling.id} har feilet med begrunnelse ${forsendelseBestilling.feilBegrunnelse}. " +
                    "Ignorer bestilling"
            }
            return
        }
        if (forsendelseBestilling.forsendelseId != null && forsendelseBestilling.skalSlettes) {
            slettForsendelse(forsendelseBestilling)
            val nyForsendelseBestilling =
                ForsendelseBestilling(
                    gjelder = forsendelseBestilling.gjelder,
                    mottaker = forsendelseBestilling.mottaker,
                    rolletype = forsendelseBestilling.rolletype,
                    språkkode = forsendelseBestilling.språkkode,
                    dokumentmal = forsendelseBestilling.dokumentmal,
                    forsendelsestype = forsendelseBestilling.forsendelsestype,
                    unikReferanse = forsendelseBestilling.unikReferanse,
                    vedtak = forsendelseBestilling.vedtak,
                    stønadstype = forsendelseBestilling.stønadstype,
                    barn = forsendelseBestilling.barn,
                    batchId = forsendelseBestilling.batchId,
                )
            opprettForsendelseTilBidragDokument(nyForsendelseBestilling)
        } else {
            opprettForsendelseTilBidragDokument(forsendelseBestilling)
        }
    }

    fun distribuerForsendelse(forsendelseBestilling: ForsendelseBestilling) {
        LOGGER.info {
            "Bestiller distribusjon av forsendelse ${forsendelseBestilling.forsendelseId} " +
                "til gjelder ${forsendelseBestilling.gjelder} " +
                "og mottaker ${forsendelseBestilling.mottaker} med rolle ${forsendelseBestilling.rolletype} " +
                "for barnId ${forsendelseBestilling.barn.id}"
        }
        val distribuerJournalpostResponse =
            bidragDokumentForsendelseConsumer.distribuerForsendelse(
                forsendelseBestilling.batchId,
                forsendelseBestilling.forsendelseId!!,
            )
        LOGGER.info {
            "Distribuerte forsendelse ${forsendelseBestilling.forsendelseId} " +
                "med journalpostId ${distribuerJournalpostResponse.journalpostId.numeric} " +
                "relatert til sak ${forsendelseBestilling.barn.saksnummer}"
        }
        forsendelseBestilling.journalpostId = distribuerJournalpostResponse.journalpostId.numeric
        forsendelseBestilling.distribuertTidspunkt = Timestamp(System.currentTimeMillis())
        forsendelseBestillingRepository.save(forsendelseBestilling)
    }

    private fun opprettForsendelseTilBidragDokument(forsendelseBestilling: ForsendelseBestilling) {
        val request = mapper.tilOpprettForsendelseRequest(forsendelseBestilling) ?: return
        try {
            val forsendelseId =
                bidragDokumentForsendelseConsumer.opprettForsendelse(request)
            forsendelseBestilling.forsendelseId = forsendelseId
            forsendelseBestilling.forsendelseOpprettetTidspunkt = Timestamp(System.currentTimeMillis())
            forsendelseBestilling.feilBegrunnelse = null
            LOGGER.info {
                "Opprettet forsendelse ${forsendelseBestilling.forsendelseId} for rolle ${forsendelseBestilling.rolletype} " +
                    "relatert sak ${forsendelseBestilling.barn.saksnummer}"
            }
        } catch (e: Exception) {
            LOGGER.error(e) {
                "Feil ved opprettelse av forsendelse ${forsendelseBestilling.id} " +
                    "for rolle ${forsendelseBestilling.rolletype} " +
                    "relatert til sak ${forsendelseBestilling.barn.saksnummer}"
            }
            forsendelseBestilling.feilBegrunnelse = e.message
        }

        forsendelseBestillingRepository.save(forsendelseBestilling)
    }

    private fun slettEksisterendeBestillingerSomIkkeErOpprettet(forsendelseEntity: ForsendelseEntity) {
        val eksisterende = forsendelseEntity.forsendelseBestilling

        fun skalSlettes(bestilling: ForsendelseBestilling): Boolean =
            (bestilling.skalSlettes && bestilling.slettetTidspunkt == null) ||
                (bestilling.forsendelseId == null && bestilling.distribuertTidspunkt == null)

        eksisterende
            .filter { skalSlettes(it) }
            .forEach { slettForsendelse(it) }

        forsendelseEntity.forsendelseBestilling.removeIf { skalSlettes(it) }
    }

    private fun opprettBestillingForBidragspliktig(
        forsendelseEntity: ForsendelseEntity,
        sak: BidragssakDto,
        forsendelsestype: Forsendelsestype,
    ): ForsendelseBestilling {
        val bp = sak.bidragspliktig!!
        return opprettForsendelseBestilling(
            forsendelseEntity = forsendelseEntity,
            gjelderIdent = bp.fødselsnummer!!.verdi,
            mottakerident = bp.fødselsnummer!!.verdi,
            rolletype = Rolletype.BIDRAGSPLIKTIG,
            forsendelsestype = forsendelsestype,
        )
    }

    private fun opprettForsendelseBestilling(
        forsendelseEntity: ForsendelseEntity,
        forsendelsestype: Forsendelsestype,
        gjelderIdent: String?,
        mottakerident: String?,
        rolletype: Rolletype,
        feilBegrunnelse: String? = null,
    ): ForsendelseBestilling {
        val forsendelseBestilling =
            ForsendelseBestilling(
                rolletype = rolletype,
                gjelder = gjelderIdent,
                mottaker = mottakerident,
                dokumentmal = forsendelsestype.dokumentmal,
                språkkode = Språk.NB,
                feilBegrunnelse = feilBegrunnelse,
                unikReferanse = forsendelseEntity.unikReferanse,
                vedtak = forsendelseEntity.vedtak!!,
                stønadstype = forsendelseEntity.stønadstype,
                barn = forsendelseEntity.barn,
                batchId = forsendelseEntity.batchId,
                forsendelsestype = forsendelsestype,
            )
        val opprettetForsendelse = forsendelseBestillingRepository.save(forsendelseBestilling)
        LOGGER.info {
            "Opprettet forsendelse bestilling ${opprettetForsendelse.id} " +
                "for rolle ${opprettetForsendelse.rolletype} med dokumentmalId ${opprettetForsendelse.dokumentmal} " +
                "relatert til  ${opprettetForsendelse.dokumentmal} og sak ${opprettetForsendelse.barn.saksnummer}"
        }
        return forsendelseBestilling
    }

    private fun opprettBestillingForBidragsmottaker(
        forsendelseEntity: ForsendelseEntity,
        sak: BidragssakDto,
        forsendelsestype: Forsendelsestype,
    ): ForsendelseBestilling {
        val mottakerIdent = finnMottakerIdent(forsendelseEntity.barn, sak)
        return opprettForsendelseBestilling(
            forsendelseEntity = forsendelseEntity,
            gjelderIdent = mottakerIdent.gjelder,
            mottakerident = mottakerIdent.mottakerIdent,
            rolletype = mottakerIdent.mottakerRolle,
            feilBegrunnelse = mottakerIdent.feilBegrunnelse,
            forsendelsestype = forsendelsestype,
        )
    }

    private fun erPersonDød(personident: Personident): Boolean {
        val person = bidragPersonConsumer.hentPerson(personident)
        return person.dødsdato != null
    }

    private fun finnMottakerIdent(
        barn: Barn,
        sak: BidragssakDto,
    ): ForsendelseGjelderMottakerInfo {
        val rolleBarn = sak.roller.find { it.fødselsnummer?.verdi == barn.kravhaver }!!

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
                LOGGER.info {
                    "Fødselsnummer til RM til barn er har samme fødselsnummer som barnet ${barn.fødselsnummer!!.verdi} og barnet er over 18 år. Setter mottaker av forsendelsen til barnet"
                }
                ForsendelseGjelderMottakerInfo(
                    barn.fødselsnummer!!.verdi,
                    barn.fødselsnummer!!.verdi,
                    Rolletype.BARN,
                )
            } else {
                LOGGER.info {
                    "Fødselsnummer til RM til barn er har samme fødselsnummer som barnet ${barn.fødselsnummer!!.verdi} og men er under 18 år. Setter mottaker av forsendelsen til BM"
                }
                null
            }
        }
        // sjekker om denne reelle mottaker er verge -> forsendelse skal til RM
        if (rm.verge) {
            LOGGER.info { "RM ${rm.ident} til barn ${barn.fødselsnummer} er verge. Setter mottaker av forsendelsen til RM" }
            // Hvis reell mottaker eksisterer og er verge, sendes forsendelsen til vergen
            return ForsendelseGjelderMottakerInfo(barn.fødselsnummer!!.verdi, rm.ident.verdi, Rolletype.REELMOTTAKER)
        }

        if (!SamhandlerId(rm.ident.verdi).gyldig()) {
            LOGGER.warn {
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
            LOGGER.info {
                "RM ${rm.ident} til barnet ${barn.fødselsnummer} er barnevernsinstitusjon." +
                    " Setter mottaker av forsendelsen til RM"
            }
            return ForsendelseGjelderMottakerInfo(
                barn.fødselsnummer!!.verdi,
                rm.ident.verdi,
                Rolletype.REELMOTTAKER,
            )
        }
        LOGGER.info {
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
