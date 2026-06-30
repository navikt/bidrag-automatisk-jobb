package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter

import no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter.RapportFilSkriver.Companion.CICS_FORMAT
import no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter.RapportFilSkriver.Companion.OPPDRAG_FORMAT
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.RapportLinje
import java.time.LocalDate

/** Rapporttypene for «BP i utlandet»-filene (FFU), tilsvarende rapporttype 2, 3 og 4 i bisys FB020. */
enum class BpUtlandRapportType(
    private val headerSuffiks: String,
) {
    BREV_BESTILT(" og det er bestilt brevkopier til utenlandsk myndighet."),
    DISKRESJON(
        " eller bostedsland er ukjent, og det er diskresjon.\r\n" +
            "NB! Dataene må kontrolleres manuelt før brev sendes ut.",
    ),
    MANGLER_ADRESSE(
        " eller bostedsland er ukjent, og det mangler adresseinformasjon.\r\n" +
            "NB! Dataene må kontrolleres manuelt før brev sendes ut.",
    ),
    ;

    fun headerSuffiks(): String = headerSuffiks
}

/**
 * Rene formateringsfunksjoner som gjenskaper det eksakte filformatet til FB020-rapportene i bisys.
 * Skilt ut fra taskletene slik at formatet kan verifiseres deterministisk i tester.
 *
 * Returnerer `null` når det ikke finnes linjer å skrive (da skal ingen fil opprettes – som i bisys).
 */
object RapportFormatter {
    private const val RN = "\r\n"

    /**
     * Rapporttype 1 – fil til Bidragsreskontro. Record `1` = header, `5` = data, `9` = footer.
     * `;`-separert, `\n` som linjeskift, footer venstre-padda til 9 tegn (jf. bisys).
     */
    fun bidragsreskontro(
        linjer: List<RapportLinje>,
        dato: LocalDate,
    ): String? {
        if (linjer.isEmpty()) return null
        val datoStr = dato.format(CICS_FORMAT)
        val sb = StringBuilder()
        sb
            .append("1;")
            .append(datoStr)
            .append(";Bidrag indeksregulering ")
            .append(datoStr)
            .append("\n")
        var teller = 2
        for (linje in linjer) {
            teller++
            sb
                .append("5;")
                .append(linje.fnrBp)
                .append(";")
                .append(linje.fnrBa)
                .append(";")
                .append(linje.beløp)
                .append("\n")
        }
        sb.append("9;").append(teller.toString().padStart(9))
        return sb.toString()
    }

    /**
     * Rapporttype 2/3/4 – fil til FFU over saker hvor BP bor i utlandet. `\r\n` som linjeskift.
     */
    fun bpUtland(
        linjer: List<RapportLinje>,
        type: BpUtlandRapportType,
        dato: LocalDate,
    ): String? {
        if (linjer.isEmpty()) return null
        val sb = StringBuilder()
        sb
            .append("Indeksregulering bidrag og 18 års bidrag hvor BP bor i utlandet")
            .append(type.headerSuffiks())
            .append(RN)
            .append("Indeksdato: ")
            .append(dato.format(OPPDRAG_FORMAT))
            .append(".")
            .append(RN)
            .append(RN)

        var teller = 0
        for (linje in linjer) {
            teller++
            sb
                .append("Land: ")
                .append(linje.landkode)
                .append("   Saksnummer: ")
                .append(linje.saksnummer)
            when (type) {
                BpUtlandRapportType.BREV_BESTILT -> {
                    sb
                        .append("   Fødselsnummer BP: ")
                        .append(linje.fnrBp)
                        .append("   Fødselsnummer BA: ")
                        .append(linje.fnrBa)
                }

                BpUtlandRapportType.DISKRESJON -> {
                    sb.append("   Beløp: ").append(linje.beløp)
                }

                BpUtlandRapportType.MANGLER_ADRESSE -> {
                    sb
                        .append("   Fødselsnummer BP: ")
                        .append(linje.fnrBp)
                        .append("   Fødselsnummer BA: ")
                        .append(linje.fnrBa)
                        .append("   Beløp: ")
                        .append(linje.beløp)
                }
            }
            sb.append(RN)
        }
        sb.append(RN).append("Antall: ").append(teller)
        return sb.toString()
    }

    /**
     * Rapporttype 5 – fil til Elin med alle nye/indeksregulerte bidrag. Record `1`/`5`/`9`,
     * `;`-separert, systemets linjeskift (jf. bisys).
     */
    fun elin(
        linjer: List<RapportLinje>,
        dato: LocalDate,
        linjeskift: String = System.lineSeparator(),
    ): String? {
        if (linjer.isEmpty()) return null
        val datoStr = dato.format(CICS_FORMAT)
        val sb = StringBuilder()
        sb
            .append("1;")
            .append(datoStr)
            .append(";Bidrag indeksregulering ")
            .append(datoStr)
            .append(linjeskift)
        var teller = 2
        for (linje in linjer) {
            teller++
            sb
                .append("5;")
                .append(linje.fnrBp)
                .append(";")
                .append(linje.fnrBa)
                .append(";")
                .append(linje.beløp)
                .append(linjeskift)
        }
        sb.append("9;").append(teller)
        return sb.toString()
    }
}
