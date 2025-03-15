package no.nav.bidrag.automatiskjobb.service

import io.getunleash.Unleash
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragStønadConsumer
import no.nav.bidrag.automatiskjobb.consumer.OppgaveConsumer
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveSokRequest
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveType
import no.nav.bidrag.automatiskjobb.consumer.dto.OpprettOppgaveRequest
import no.nav.bidrag.automatiskjobb.consumer.dto.lagBeskrivelseHeader
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.stonad.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.stonad.response.StønadDto
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import org.springframework.stereotype.Service
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}
val revurderForskuddBeskrivelse = "Forskuddet skal reduseres basert på inntekt fra siste vedtak."
val enhet_farskap = "4860"
val skyldnerNav = Personident("NAV")

@Service
class OppgaveService(
    private val oppgaveConsumer: OppgaveConsumer,
    private val bidragStønadConsumer: BidragStønadConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
    private val revurderForskuddService: RevurderForskuddService,
    private val unleash: Unleash,
) {
    fun opprettRevurderForskuddOppgave(vedtakHendelse: VedtakHendelse) {
        try {
            revurderForskuddService
                .erForskuddRedusert(vedtakHendelse)
                .forEach { stønad ->
                    log.info {
                        "Sak ${stønad.saksnummer} har løpende forskudd. Opprett revurder forskudd oppgave"
                    }
                    secureLogger.info {
                        "Sak ${stønad.saksnummer} har løpende forskudd for mottaker ${stønad.bidragsmottaker}. Opprett revurder forskudd oppgave"
                    }
//                vedtakHendelse.opprettRevurderForskuddOppgave(stønad.saksnummer, stønad.bidragsmottaker)
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
        val enhet = finnEnhetsnummer(saksnummer)
        val oppgaveResponse =
            oppgaveConsumer.opprettOppgave(
                OpprettOppgaveRequest(
                    beskrivelse = lagBeskrivelseHeader(opprettetAv, enhet) + revurderForskuddBeskrivelse,
                    oppgavetype = OppgaveType.GEN,
                    tema = if (enhet_farskap == enhet) "FAR" else "BID",
                    saksreferanse = saksnummer,
                    tilordnetRessurs = finnTilordnetRessurs(),
                    tildeltEnhetsnr = enhet,
                    personident = mottaker,
                ),
            )

        log.info { "Opprettet revurder forskudd oppgave ${oppgaveResponse.id} for sak $saksnummer" }
        secureLogger.info { "Opprettet revurder forskudd oppgave $oppgaveResponse for sak $saksnummer og bidragsmottaker $mottaker" }
    }

    fun VedtakHendelse.finnTilordnetRessurs(): String? {
        val vedtakEnhet = enhetsnummer!!.verdi
        if (!vedtakEnhet.erKlageinstans()) return opprettetAv
        return null
    }

    fun VedtakHendelse.finnEnhetsnummer(saksnummer: String): String {
        val vedtakEnhet = enhetsnummer!!.verdi
        if (!vedtakEnhet.erKlageinstans()) return vedtakEnhet
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
            log.info { "Fant revurder forskudd oppgave for sak $saksnummer og bidragsmottaker $mottaker. Oppretter ikke ny oppgave" }
            secureLogger.info {
                "Fant revurder forskudd oppgave $revurderForskuddOppgave for sak $saksnummer og bidragsmottaker $mottaker. Oppretter ikke ny oppgave"
            }
            return true
        }
        return false
    }

    private fun hentLøpendeForskuddForSak(
        saksnummer: String,
        søknadsbarnIdent: String,
    ): StønadDto? =
        bidragStønadConsumer.hentHistoriskeStønader(
            HentStønadHistoriskRequest(
                type = Stønadstype.FORSKUDD,
                sak = Saksnummer(saksnummer),
                skyldner = skyldnerNav,
                kravhaver = Personident(søknadsbarnIdent),
                gyldigTidspunkt = LocalDateTime.now(),
            ),
        )
}
