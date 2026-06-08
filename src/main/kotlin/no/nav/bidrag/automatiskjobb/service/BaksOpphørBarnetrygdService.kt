package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.domene.BarnetrygdBisysMelding
import no.nav.bidrag.automatiskjobb.domene.BarnetrygdEndretType
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.rolle.Rolletype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger { }

@Service
class BaksOpphørBarnetrygdService(
    private val barnRepository: BarnRepository,
    private val oppgaveService: OppgaveService,
    private val bidragSakConsumer: BidragSakConsumer,
) {
    @Transactional
    fun behandleBarnetrygdHendelse(hendelse: BarnetrygdBisysMelding) {
        hendelse.barn.forEach { barnhendelse ->
            val barnMedForskudd =
                barnRepository
                    .finnLøpendeForskuddForBarn(dato = LocalDate.now(), kravhaver = barnhendelse.ident)
                    .firstOrNull()
            val barnMedBidrag =
                barnRepository
                    .finnLøpendeBidragForBarn(dato = LocalDate.now(), kravhaver = barnhendelse.ident)
                    .firstOrNull()
            if ((barnMedForskudd != null || barnMedBidrag != null) &&
                barnhendelse.årsakskode in listOf(BarnetrygdEndretType.RO, BarnetrygdEndretType.RR)
            ) {
                // Kall bidragSakConsumer for å sjekke om hendelse.søker har rollen BM i barnets sak.
                val søkerErbMbPISak =
                    bidragSakConsumer
                        .hentSak(
                            barnMedForskudd?.saksnummer ?: barnMedBidrag!!.saksnummer,
                        ).roller
                        .filter { it.type == Rolletype.BIDRAGSMOTTAKER || it.type == Rolletype.BIDRAGSPLIKTIG }
                        .any { it.fødselsnummer?.verdi == hendelse.søker }

                if (søkerErbMbPISak) {
                    secureLogger.info {
                        "Vedtak om reduksjon/opphør av barnetrygd mottatt for barn med forskudd eller bidrag: " +
                            "${barnhendelse.ident}, hendelse: $hendelse"
                    }
                    // lag oppgave for å revurdere forskudd og bidrag for barnet
                    oppgaveService.opprettRevurderForskuddOgBidragOppgave(
                        saksnummer = barnMedForskudd?.saksnummer ?: barnMedBidrag!!.saksnummer,
                        mottaker = hendelse.søker,
                        kravhaver = barnhendelse.ident,
                        fom = barnhendelse.fom,
                    )
                } else {
                    secureLogger.info {
                        "Vedtak om reduksjon/opphør av barnetrygd mottatt for person som ikke er bidragspliktig eller " +
                            "bidragsmottaker i barnets sak: " +
                            "${barnhendelse.ident}, hendelse: $hendelse"
                    }
                }
            }
        }
    }
}
