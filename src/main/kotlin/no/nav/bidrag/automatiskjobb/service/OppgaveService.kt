package no.nav.bidrag.automatiskjobb.service

import io.getunleash.Unleash
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.combinedLogger
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.OppgaveConsumer
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveDto
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveSokRequest
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveType
import no.nav.bidrag.automatiskjobb.consumer.dto.OpprettOppgaveRequest
import no.nav.bidrag.automatiskjobb.consumer.dto.lagBeskrivelseHeader
import no.nav.bidrag.automatiskjobb.consumer.dto.lagBeskrivelseHeaderAutomatiskJobb
import no.nav.bidrag.automatiskjobb.domene.Endringsmelding
import no.nav.bidrag.automatiskjobb.domene.erAdresseendring
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.model.AdresseEndretResultat
import no.nav.bidrag.automatiskjobb.service.model.ForskuddRedusertResultat
import no.nav.bidrag.automatiskjobb.utils.erForskudd
import no.nav.bidrag.automatiskjobb.utils.oppgaveAldersjusteringBeskrivelse
import no.nav.bidrag.automatiskjobb.utils.revurderForskuddBeskrivelseAdresseendring
import no.nav.bidrag.automatiskjobb.utils.tilOppgaveBeskrivelse
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.felles.enhet_farskap
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

val opprettRevurderForskuddOppgaveToggleName = "automatiskjobb.opprett-revurder-forskudd-oppgave"

@Service
class OppgaveService(
    private val oppgaveConsumer: OppgaveConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
    private val revurderForskuddService: RevurderForskuddService,
    private val unleash: Unleash,
) {
    fun sjekkOgOpprettRevurderForskuddOppgaveEtterBarnFlyttetFraBM(hendelse: Endringsmelding) {
        if (hendelse.erAdresseendring) {
            try {
                secureLogger.info {
                    "Sjekker for person om barn mottar forskudd og fortsatt bor hos BM etter adresseendring i hendelse $hendelse"
                }
                revurderForskuddService.skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(hendelse.aktørid).forEach {
                    if (unleash.isEnabled(opprettRevurderForskuddOppgaveToggleName)) {
                        it.opprettRevurderForskuddOppgaveEtterAdresseEndring()
                    } else {
                        log.info { "Feature toggle $opprettRevurderForskuddOppgaveToggleName er skrudd av. Oppretter ikke oppgave" }
                        secureLogger.info {
                            "Feature toggle $opprettRevurderForskuddOppgaveToggleName er skrudd av. Oppretter ikke oppgave for $it"
                        }
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Det skjedde en feil ved sjekk om BM fortsatt skal motta forskudd for barn fra hendelse" }
                secureLogger.error(
                    e,
                ) { "Det skjedde en feil ved sjekk om BM fortsatt skal motta forskudd for barn fra hendelse $hendelse" }
            }
        }
    }

    fun opprettRevurderForskuddOppgave(vedtakHendelse: VedtakHendelse) {
        try {
            if (vedtakHendelse.erForskudd()) return
            if (vedtakHendelse.kilde == Vedtakskilde.AUTOMATISK) return
            log.info {
                "Sjekker om det skal opprettes revurder forskudd oppgave for vedtak ${vedtakHendelse.id} med fattet i system ${vedtakHendelse.kildeapplikasjon} av ${vedtakHendelse.opprettetAv}"
            }
            secureLogger.info { "Sjekker om det skal opprettes revurder forskudd oppgave for hendelse $vedtakHendelse" }
            revurderForskuddService
                .erForskuddRedusert(vedtakHendelse)
                .forEach { resultat ->
                    combinedLogger.info {
                        "Forskuddet skal reduseres i sak ${resultat.saksnummer} for mottaker ${resultat.bidragsmottaker} og kravhaver ${resultat.gjelderBarn}. Opprett revurder forskudd oppgave"
                    }
                    vedtakHendelse.opprettRevurderForskuddOppgave(resultat)
                    return // Opprett kun en oppgave per sak
                }
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved sjekk for om det skal opprettes revurder forskudd oppgave" }
        }
    }

    private fun AdresseEndretResultat.opprettRevurderForskuddOppgaveEtterAdresseEndring() {
        if (finnesDetRevurderForskuddOppgaveISak()) return
        val oppgaveResponse =
            oppgaveConsumer.opprettOppgave(
                OpprettOppgaveRequest(
                    beskrivelse = lagBeskrivelseHeaderAutomatiskJobb() + revurderForskuddBeskrivelseAdresseendring,
                    oppgavetype = OppgaveType.GEN,
                    tema = if (enhet_farskap == enhet) "FAR" else "BID",
                    saksreferanse = saksnummer,
                    tildeltEnhetsnr = enhet,
                    personident = gjelderBarn,
                ),
            )

        log.info {
            "Opprettet revurder forskudd etter adresseendring oppgave ${oppgaveResponse.id} for sak $saksnummer og enhet $enhet"
        }
        secureLogger.info {
            "Opprettet revurder forskudd etter adresseendring oppgave $oppgaveResponse for sak $saksnummer, enhet $enhet og barn $gjelderBarn"
        }
    }

    private fun finnEksisterendeOppgaveForManuellAldersjusteringISak(aldersjustering: Aldersjustering): OppgaveDto? {
        val oppgaver =
            oppgaveConsumer.hentOppgave(
                OppgaveSokRequest()
                    .søkForGenerellOppgave()
                    .leggTilAktoerId(aldersjustering.barn.kravhaver)
                    .leggTilSaksreferanse(aldersjustering.barn.saksnummer),
            )
        return oppgaver.oppgaver.find { it.beskrivelse!!.contains(aldersjustering.tilManuellOppgaveBeskrivelse()) }
    }

    fun slettOppgave(oppgaveId: Int): Long {
        val oppgave = oppgaveConsumer.hentOppgaveForId(oppgaveId)
        val slettetOppgave = oppgaveConsumer.slettOppgave(oppgaveId, oppgave.versjon)
        log.info { "Slettet oppgave med id $oppgaveId: $slettetOppgave" }
        return slettetOppgave.id
    }

    fun opprettOppgaveForManuellAldersjustering(aldersjustering: Aldersjustering): Int {
        val eksisterendeOppgave = finnEksisterendeOppgaveForManuellAldersjusteringISak(aldersjustering)
        if (eksisterendeOppgave != null) {
            combinedLogger.info {
                "Fant eksisterende oppgave $eksisterendeOppgave for " +
                    "manuell aldersjustering ${aldersjustering.id} i sak ${aldersjustering.barn.saksnummer} og barn ${aldersjustering.barn.saksnummer}. " +
                    "Oppretter ikke ny oppgave"
            }
            return eksisterendeOppgave.id.toInt()
        }
        val barn = aldersjustering.barn
        val enhet = finnEierfogd(barn.saksnummer)
        val oppgaveResponse =
            oppgaveConsumer.opprettOppgave(
                OpprettOppgaveRequest(
                    beskrivelse =
                        lagBeskrivelseHeaderAutomatiskJobb() +
                            aldersjustering.tilManuellOppgaveBeskrivelse(),
                    oppgavetype = OppgaveType.GEN,
                    saksreferanse = barn.saksnummer,
                    tema = if (enhet_farskap == enhet) "FAR" else "BID",
                    tildeltEnhetsnr = enhet,
                    personident = barn.kravhaver,
                ),
            )
        log.info { "Opprettet oppgave ${oppgaveResponse.id} for sak ${barn.saksnummer} og enhet $enhet" }
        secureLogger.info { "Opprettet oppgave $oppgaveResponse for barn $barn, enhet $enhet." }

        return oppgaveResponse.id.toInt()
    }

    private fun VedtakHendelse.opprettRevurderForskuddOppgave(forskuddRedusertResultat: ForskuddRedusertResultat) {
        if (finnesDetRevurderForskuddOppgaveISak(forskuddRedusertResultat)) return
        val enhet = finnEierfogd(forskuddRedusertResultat.saksnummer)
        val oppgaveResponse =
            oppgaveConsumer.opprettOppgave(
                OpprettOppgaveRequest(
                    beskrivelse = lagBeskrivelseHeader(opprettetAv, enhet) + forskuddRedusertResultat.tilOppgaveBeskrivelse(),
                    oppgavetype = OppgaveType.GEN,
                    tema = if (enhet_farskap == enhet) "FAR" else "BID",
                    saksreferanse = forskuddRedusertResultat.saksnummer,
                    tilordnetRessurs = finnTilordnetRessurs(forskuddRedusertResultat.saksnummer),
                    tildeltEnhetsnr = enhet,
                    personident = forskuddRedusertResultat.bidragsmottaker,
                ),
            )

        log.info {
            "Opprettet revurder forskudd oppgave ${oppgaveResponse.id} for sak ${forskuddRedusertResultat.saksnummer} og enhet $enhet"
        }
        secureLogger.info {
            "Opprettet revurder forskudd oppgave $oppgaveResponse for sak ${forskuddRedusertResultat.saksnummer}, enhet $enhet og bidragsmottaker ${forskuddRedusertResultat.bidragsmottaker}"
        }
    }

    private fun VedtakHendelse.finnTilordnetRessurs(saksnummer: String): String? {
        val vedtakEnhet = enhetsnummer!!.verdi
        val eierFogd = finnEierfogd(saksnummer)
        if (!vedtakEnhet.erKlageinstans() && eierFogd == vedtakEnhet) return opprettetAv
        return null
    }

    private fun finnEierfogd(saksnummer: String): String {
        val sak = bidragSakConsumer.hentSak(saksnummer)
        return sak.eierfogd.verdi
    }

    private fun String.erKlageinstans() = startsWith("42")

    private fun AdresseEndretResultat.finnesDetRevurderForskuddOppgaveISak(): Boolean {
        val oppgaver =
            oppgaveConsumer.hentOppgave(
                OppgaveSokRequest()
                    .søkForGenerellOppgave()
                    .leggTilAktoerId(gjelderBarn)
                    .leggTilSaksreferanse(saksnummer),
            )
        val revurderForskuddOppgave = oppgaver.oppgaver.find { it.beskrivelse!!.contains(revurderForskuddBeskrivelseAdresseendring) }
        if (revurderForskuddOppgave != null) {
            combinedLogger.info {
                "Fant revurder forskudd etter adresseendring oppgave $revurderForskuddOppgave for sak $saksnummer og barn $gjelderBarn. Oppretter ikke ny oppgave"
            }
            return true
        }
        return false
    }

    fun VedtakHendelse.finnesDetRevurderForskuddOppgaveISak(forskuddRedusertResultat: ForskuddRedusertResultat): Boolean {
        val oppgaver =
            oppgaveConsumer.hentOppgave(
                OppgaveSokRequest()
                    .søkForGenerellOppgave()
                    .leggTilSaksreferanse(forskuddRedusertResultat.saksnummer),
            )
        val revurderForskuddOppgave = oppgaver.oppgaver.find { it.beskrivelse!!.contains(forskuddRedusertResultat.tilOppgaveBeskrivelse()) }
        if (revurderForskuddOppgave != null) {
            combinedLogger.info {
                "Fant revurder forskudd oppgave $revurderForskuddOppgave for sak ${forskuddRedusertResultat.saksnummer} og bidragsmottaker ${forskuddRedusertResultat.bidragsmottaker}. Oppretter ikke ny oppgave"
            }
            return true
        }
        return false
    }

    private fun Aldersjustering.tilManuellOppgaveBeskrivelse() =
        oppgaveAldersjusteringBeskrivelse.replace(
            "{}",
            begrunnelseVisningsnavn.firstOrNull() ?: "",
        )
}
