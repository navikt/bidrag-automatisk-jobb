package no.nav.bidrag.automatiskjobb.service

import no.nav.bidrag.automatiskjobb.consumer.BidragReskontroConsumer
import no.nav.bidrag.domene.sak.Saksnummer
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ReskontroService(
    private val bidragReskontroConsumer: BidragReskontroConsumer,
) {
    fun finnesForskuddForSakPeriode(
        saksnummer: Saksnummer,
        perioder: List<LocalDate>,
    ): Boolean {
        val transaksjoner = bidragReskontroConsumer.hentTransaksjonerForBidragssak(saksnummer)
        return transaksjoner.transaksjoner.any {
            it.transaksjonskode == "A4" &&
                perioder.any { periode -> periode.equals(it.periode?.fom) } &&
                it.beløp != null && it.beløp!! > BigDecimal.ZERO
        }
    }
}
