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
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.saksnummer
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}
val revurderForskuddBeskrivelse = "Revurder forskudd basert på inntekt fra siste vedtak."
val enhet_farskap = "4860"
val skyldnerNav = Personident("NAV")

fun VedtakHendelse.erForskudd() = stønadsendringListe?.any { it.type == Stønadstype.FORSKUDD } == true

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
            revurderForskuddService
                .erForskuddRedusert(vedtakHendelse)
                .forEach { resultat ->
                    combinedLogger.info {
                        "Forskuddet skal reduseres i sak ${resultat.saksnummer} for mottaker ${resultat.bidragsmottaker} og kravhaver ${resultat.gjelderBarn}. Opprett revurder forskudd oppgave"
                    }
                    if (unleash.isEnabled("automatiskjobb.opprett-revurder-forskudd-oppgave")) {
                        vedtakHendelse.opprettRevurderForskuddOppgave(resultat.saksnummer, resultat.bidragsmottaker)
                    }
                    return // Opprett kun en oppgave per sak
                }
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved sjekk for om det skal opprettes revurder forskudd oppgave" }
        }
    }

    fun VedtakHendelse.opprettRevurderForskuddOppgave(
        saksnummer: String,
        mottaker: String,
    ) {
        if (finnesDetRevurderForskuddOppgaveISak(saksnummer, mottaker)) return
        val enhet = finnEierfogd(saksnummer)
        val oppgaveResponse =
            oppgaveConsumer.opprettOppgave(
                OpprettOppgaveRequest(
                    beskrivelse = lagBeskrivelseHeader(opprettetAv, enhet) + revurderForskuddBeskrivelse,
                    oppgavetype = OppgaveType.GEN,
                    tema = if (enhet_farskap == enhet) "FAR" else "BID",
                    saksreferanse = saksnummer,
                    tilordnetRessurs = finnTilordnetRessurs(saksnummer),
                    tildeltEnhetsnr = enhet,
                    personident = mottaker,
                ),
            )

        log.info { "Opprettet revurder forskudd oppgave ${oppgaveResponse.id} for sak $saksnummer og enhet $enhet" }
        secureLogger.info {
            "Opprettet revurder forskudd oppgave $oppgaveResponse for sak $saksnummer, enhet $enhet og bidragsmottaker $mottaker"
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

    fun VedtakHendelse.finnesDetRevurderForskuddOppgaveISak(
        saksnummer: String,
        mottaker: String,
    ): Boolean {
        val oppgaver =
            oppgaveConsumer.hentOppgave(
                OppgaveSokRequest()
                    .søkForGenerellOppgave()
                    .leggTilSaksreferanse(saksnummer),
            )
        val revurderForskuddOppgave = oppgaver.oppgaver.find { it.beskrivelse!!.contains(revurderForskuddBeskrivelse) }
        if (revurderForskuddOppgave != null) {
            combinedLogger.info {
                "Fant revurder forskudd oppgave $revurderForskuddOppgave for sak $saksnummer og bidragsmottaker $mottaker. Oppretter ikke ny oppgave"
            }
            return true
        }
        return false
    }
}
