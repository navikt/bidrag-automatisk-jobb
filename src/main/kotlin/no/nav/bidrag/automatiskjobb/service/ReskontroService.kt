package no.nav.bidrag.automatiskjobb.service

import com.google.common.collect.ImmutableList
import no.nav.bidrag.automatiskjobb.consumer.BidragReskontroConsumer
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.collections.contains

@Service
class ReskontroService(
    private val bidragReskontroConsumer: BidragReskontroConsumer,
) {
    private val redusertBidragBMSkylderBp: String = "B4"
    private val redusertBidragB18SkylderBP: String = "D4"
    private val transaksjonskodeAvregning: MutableCollection<String> =
        ImmutableList.of(
            redusertBidragBMSkylderBp,
            redusertBidragB18SkylderBP,
        )

    fun finnesForskuddForSakPeriode(
        saksnummer: Saksnummer,
        perioder: List<LocalDate>,
    ): Boolean {
        val transaksjoner = bidragReskontroConsumer.hentTransaksjonerForBidragssak(saksnummer)
        return transaksjoner.transaksjoner.any {
            it.transaksjonskode == "A4" &&
                it.beløp != null &&
                it.beløp != BigDecimal.ZERO &&
                perioder.any { periode -> periode.withDayOfMonth(1) == it.periode?.fom?.withDayOfMonth(1) }
        }
    }

    fun hentSumAvregningForStønad(
        stønadsid: Stønadsid,
        vedtakstidspunkt: LocalDate,
    ): BigDecimal {
        try {
            val transaksjoner = bidragReskontroConsumer.hentTransaksjonerForBidragssak(stønadsid.sak) ?: return BigDecimal.ZERO

            val transaksjonerMedAvskrivning =
                transaksjoner.transaksjoner
                    .filter { isAvskrivning(it) }

            val sumAvregning =
                transaksjonerMedAvskrivning
                    .filter { it.skyldner == stønadsid.skyldner && it.barn == stønadsid.kravhaver && it.dato == vedtakstidspunkt }
                    .sumOf { it.beløp ?: BigDecimal.ZERO }

            secureLogger.info {
                "Fant sum avregning $sumAvregning for stønad $stønadsid og vedtakstidspunkt $vedtakstidspunkt"
            }
            return sumAvregning
        } catch (e: Exception) {
            secureLogger.error(
                e,
            ) { "Det skjedde en feil ved uthenting av sum avregning for stønad $stønadsid og vedtakstidspunkt $vedtakstidspunkt" }
            return BigDecimal.ZERO
        }
    }

    private fun isAvskrivning(transaksjon: TransaksjonDto): Boolean = transaksjonskodeAvregning.contains(transaksjon.transaksjonskode)
}
