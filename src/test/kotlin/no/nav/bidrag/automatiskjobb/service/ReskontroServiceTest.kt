package no.nav.bidrag.automatiskjobb.service

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.automatiskjobb.consumer.BidragReskontroConsumer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonDto
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonerDto
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ReskontroServiceTest {
    private val bidragReskontroConsumer = mockk<BidragReskontroConsumer>()
    private lateinit var reskontroService: ReskontroService

    @BeforeEach
    fun setUp() {
        reskontroService = ReskontroService(bidragReskontroConsumer)
    }

    @Test
    fun `skal returnere true hvis en transaksjon matcher alle kriterier`() {
        every { bidragReskontroConsumer.hentTransaksjonerForBidragssak(any()) } returns
            TransaksjonerDto(
                listOf(
                    opprettTransaksjon(Datoperiode(LocalDate.of(2023, 1, 1), null), BigDecimal.valueOf(100), "A4"),
                ),
            )

        val resultat = reskontroService.finnesForskuddForSakPeriode(Saksnummer("123"), listOf(LocalDate.of(2023, 1, 25)))
        assertTrue(resultat)
    }

    private fun opprettTransaksjon(
        datoperiode: Datoperiode?,
        beløp: BigDecimal?,
        transaksjonskode: String,
    ): TransaksjonDto =
        TransaksjonDto(
            transaksjonskode = transaksjonskode,
            periode = datoperiode,
            beløp = beløp,
            transaksjonsid = null,
            beskrivelse = null,
            dato = null,
            skyldner = null,
            mottaker = null,
            restBeløp = null,
            beløpIOpprinneligValuta = null,
            valutakode = "NOK",
            saksnummer = null,
            barn = null,
            delytelsesid = null,
            søknadstype = null,
        )

    @Test
    fun `skal returnere false hvis ingen transaksjoner matcher`() {
        every { bidragReskontroConsumer.hentTransaksjonerForBidragssak(any()) } returns
            TransaksjonerDto(
                listOf(
                    opprettTransaksjon(Datoperiode(LocalDate.of(2023, 1, 1), null), BigDecimal.valueOf(100), "B1"),
                ),
            )

        val resultat = reskontroService.finnesForskuddForSakPeriode(Saksnummer("123"), listOf(LocalDate.of(2023, 1, 1)))
        assertFalse(resultat)
    }

    @Test
    fun `skal returnere false hvis beløp er null eller null eller 0`() {
        every { bidragReskontroConsumer.hentTransaksjonerForBidragssak(any()) } returns
            TransaksjonerDto(
                listOf(
                    opprettTransaksjon(Datoperiode(LocalDate.of(2023, 1, 1), null), null, "A4"),
                    opprettTransaksjon(Datoperiode(LocalDate.of(2023, 1, 1), null), BigDecimal.ZERO, "A4"),
                ),
            )

        val resultat = reskontroService.finnesForskuddForSakPeriode(Saksnummer("123"), listOf(LocalDate.of(2023, 1, 1)))
        assertFalse(resultat)
    }

    @Test
    fun `skal returnere false hvis periode er null`() {
        every { bidragReskontroConsumer.hentTransaksjonerForBidragssak(any()) } returns
            TransaksjonerDto(
                listOf(
                    opprettTransaksjon(null, BigDecimal.valueOf(100), "A4"),
                ),
            )

        val resultat = reskontroService.finnesForskuddForSakPeriode(Saksnummer("123"), listOf(LocalDate.of(2023, 1, 1)))
        assertFalse(resultat)
    }

    @Test
    fun `skal returnere false hvis listen med transaksjoner er tom`() {
        every { bidragReskontroConsumer.hentTransaksjonerForBidragssak(any()) } returns TransaksjonerDto(emptyList())

        val resultat = reskontroService.finnesForskuddForSakPeriode(Saksnummer("123"), listOf(LocalDate.of(2023, 1, 1)))
        assertFalse(resultat)
    }

    @Test
    fun `skal returnere true hvis en av flere perioder matcher`() {
        every { bidragReskontroConsumer.hentTransaksjonerForBidragssak(any()) } returns
            TransaksjonerDto(
                listOf(
                    opprettTransaksjon(Datoperiode(LocalDate.of(2023, 3, 1), null), BigDecimal.valueOf(100), "A4"),
                ),
            )

        val resultat =
            reskontroService.finnesForskuddForSakPeriode(
                Saksnummer("123"),
                listOf(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1), LocalDate.of(2023, 3, 1)),
            )
        assertTrue(resultat)
    }

    @Test
    fun `skal returnere false hvis ingen av flere perioder matcher`() {
        every { bidragReskontroConsumer.hentTransaksjonerForBidragssak(any()) } returns
            TransaksjonerDto(
                listOf(
                    opprettTransaksjon(Datoperiode(LocalDate.of(2023, 3, 1), null), BigDecimal.valueOf(100), "A4"),
                ),
            )

        val resultat =
            reskontroService.finnesForskuddForSakPeriode(
                Saksnummer("123"),
                listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1)),
            )
        assertFalse(resultat)
    }
}
