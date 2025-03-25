package no.nav.bidrag.automatiskjobb.service

import io.getunleash.Unleash
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.combinedLogger
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.OppgaveConsumer
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveSokRequest
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveType
import no.nav.bidrag.automatiskjobb.consumer.dto.OpprettOppgaveRequest
import no.nav.bidrag.automatiskjobb.consumer.dto.lagBeskrivelseHeader
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}
val revurderForskuddBeskrivelse = "Revurder forskudd basert på inntekt fra nytt vedtak om barnebidrag."
val revurderForskuddBeskrivelseSærbidrag = "Revurder forskudd basert på inntekt fra nytt vedtak om særbidrag."
val enhet_farskap = "4860"
val skyldnerNav = Personident("NAV")

fun VedtakHendelse.erForskudd() = stønadsendringListe?.any { it.type == Stønadstype.FORSKUDD } == true

val opprettRevurderForskuddOppgaveToggleName = "automatiskjobb.opprett-revurder-forskudd-oppgave"

fun ForskuddRedusertResultat.tilOppgaveBeskrivelse() =
    if (engangsbeløptype ==
        Engangsbeløptype.SÆRBIDRAG
    ) {
        revurderForskuddBeskrivelseSærbidrag
    } else {
        revurderForskuddBeskrivelse
    }

@Service
class OppgaveService(
    private val oppgaveConsumer: OppgaveConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
    private val revurderForskuddService: RevurderForskuddService,
    private val unleash: Unleash,
) {
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
                    if (unleash.isEnabled(opprettRevurderForskuddOppgaveToggleName)) {
                        vedtakHendelse.opprettRevurderForskuddOppgave(resultat)
                    } else {
                        log.info { "Feature toggle $opprettRevurderForskuddOppgaveToggleName er skrudd av. Oppretter ikke oppgave" }
                    }
                    return // Opprett kun en oppgave per sak
                }
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved sjekk for om det skal opprettes revurder forskudd oppgave" }
        }
    }

    fun VedtakHendelse.opprettRevurderForskuddOppgave(forskuddRedusertResultat: ForskuddRedusertResultat) {
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

    fun VedtakHendelse.finnTilordnetRessurs(saksnummer: String): String? {
        val vedtakEnhet = enhetsnummer!!.verdi
        val eierFogd = finnEierfogd(saksnummer)
        if (!vedtakEnhet.erKlageinstans() && eierFogd == vedtakEnhet) return opprettetAv
        return null
    }

    fun finnEierfogd(saksnummer: String): String {
        val sak = bidragSakConsumer.hentSak(saksnummer)
        return sak.eierfogd.verdi
    }

    fun String.erKlageinstans() = startsWith("42")

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
}
