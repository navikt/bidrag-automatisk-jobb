package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.RapportLinje
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FilformatererTest {
    private val dato = LocalDate.of(2026, 7, 1)

    private fun linje(
        saksnummer: String = "2600001",
        fnrBp: String = "11111111111",
        fnrBa: String = "22222222222",
        beløp: BigDecimal = BigDecimal("1140"),
        landkode: String = "NO",
    ) = RapportLinje(saksnummer, fnrBp, fnrBa, beløp, landkode)

    @Test
    fun `bidragsreskontro skriver record 1-5-9 med venstre-padda antall`() {
        val resultat =
            Filformaterer.bidragsreskontro(
                listOf(
                    linje(fnrBp = "99999999999", fnrBa = "88888888888", beløp = BigDecimal("1140")),
                    linje(fnrBp = "77777777777", fnrBa = "66666666666", beløp = BigDecimal("2000")),
                ),
                dato,
            )

        resultat shouldBe
            "1;20260701;Bidrag indeksregulering 20260701\n" +
            "5;99999999999;88888888888;1140\n" +
            "5;77777777777;66666666666;2000\n" +
            "9;        4"
    }

    @Test
    fun `bidragsreskontro returnerer null for tom liste`() {
        Filformaterer.bidragsreskontro(emptyList(), dato).shouldBeNull()
    }

    @Test
    fun `bpUtland brev bestilt skriver fnr BP og BA med CRLF`() {
        val resultat = Filformaterer.bpUtland(listOf(linje(landkode = "SE")), BpUtlandRapportType.BREV_BESTILT, dato)

        resultat shouldBe
            "Indeksregulering bidrag og 18 års bidrag hvor BP bor i utlandet" +
            " og det er bestilt brevkopier til utenlandsk myndighet.\r\n" +
            "Indeksdato: 01.07.2026.\r\n\r\n" +
            "Land: SE   Saksnummer: 2600001   Fødselsnummer BP: 11111111111   Fødselsnummer BA: 22222222222\r\n" +
            "\r\nAntall: 1"
    }

    @Test
    fun `bpUtland diskresjon skriver kun beløp`() {
        val resultat =
            Filformaterer.bpUtland(
                listOf(linje(landkode = "SE", beløp = BigDecimal("1140"))),
                BpUtlandRapportType.DISKRESJON,
                dato,
            )

        resultat shouldBe
            "Indeksregulering bidrag og 18 års bidrag hvor BP bor i utlandet" +
            " eller bostedsland er ukjent, og det er diskresjon.\r\n" +
            "NB! Dataene må kontrolleres manuelt før brev sendes ut.\r\n" +
            "Indeksdato: 01.07.2026.\r\n\r\n" +
            "Land: SE   Saksnummer: 2600001   Beløp: 1140\r\n" +
            "\r\nAntall: 1"
    }

    @Test
    fun `bpUtland mangler adresse skriver fnr og beløp`() {
        val resultat =
            Filformaterer.bpUtland(
                listOf(linje(landkode = "SE", beløp = BigDecimal("1140"))),
                BpUtlandRapportType.MANGLER_ADRESSE,
                dato,
            )

        resultat shouldBe
            "Indeksregulering bidrag og 18 års bidrag hvor BP bor i utlandet" +
            " eller bostedsland er ukjent, og det mangler adresseinformasjon.\r\n" +
            "NB! Dataene må kontrolleres manuelt før brev sendes ut.\r\n" +
            "Indeksdato: 01.07.2026.\r\n\r\n" +
            "Land: SE   Saksnummer: 2600001   Fødselsnummer BP: 11111111111   Fødselsnummer BA: 22222222222   Beløp: 1140\r\n" +
            "\r\nAntall: 1"
    }

    @Test
    fun `bpUtland returnerer null for tom liste`() {
        Filformaterer.bpUtland(emptyList(), BpUtlandRapportType.BREV_BESTILT, dato).shouldBeNull()
    }

    @Test
    fun `elin skriver record 1-5-9 uten padding`() {
        val resultat =
            Filformaterer.elin(
                listOf(
                    linje(fnrBp = "99999999999", fnrBa = "88888888888", beløp = BigDecimal("1140")),
                ),
                dato,
                linjeskift = "\n",
            )

        resultat shouldBe
            "1;20260701;Bidrag indeksregulering 20260701\n" +
            "5;99999999999;88888888888;1140\n" +
            "9;3"
    }

    @Test
    fun `elin returnerer null for tom liste`() {
        Filformaterer.elin(emptyList(), dato).shouldBeNull()
    }
}
