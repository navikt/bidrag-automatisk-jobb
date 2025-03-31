package no.nav.bidrag.automatiskjobb.domene

import java.time.LocalDate

data class Endringsmelding(
    val aktørid: String,
    val personidenter: Set<String>,
    val adresseendring: AdresseEndring? = null,
    val opplysningstype: Opplysningstype = Opplysningstype.UKJENT,
) {
    data class AdresseEndring(
        val type: Opplysningstype,
        val flyttedato: LocalDate? = null,
        val utflytting: Utflytting? = null,
        val innflytting: Innflytting? = null,
    )

    enum class Opplysningstype {
        ADRESSEBESKYTTELSE,
        BOSTEDSADRESSE,
        DOEDSFALL,
        FOEDSEL,
        FOLKEREGISTERIDENTIFIKATOR,
        INNFLYTTING_TIL_NORGE,
        KONTAKTADRESSE,
        NAVN,
        OPPHOLDSADRESSE,
        SIVILSTAND,
        UTFLYTTING_FRA_NORGE,
        VERGEMAAL_ELLER_FREMTIDSFULLMAKT,
        KONTOENDRING,
        UKJENT,
    }
}

data class Utflytting(
    val tilflyttingsland: String? = null,
    val tilflyttingsstedIUtlandet: String? = null,
    val utflyttingsdato: LocalDate? = null,
)

data class Innflytting(
    val fraflyttingsland: String? = null,
    val fraflyttingsstedIUtlandet: String? = null,
)
