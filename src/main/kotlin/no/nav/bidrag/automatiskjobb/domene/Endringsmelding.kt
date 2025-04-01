package no.nav.bidrag.automatiskjobb.domene

import java.time.LocalDate

data class Endringsmelding(
    val aktørid: String,
    val personidenter: Set<String>,
    val endringer: List<Endring> = emptyList(),
) {
    data class Endring(
        val adresseendring: Adresseendring? = null,
        val sivilstandendring: Sivilstandendring? = null,
        val opplysningstype: Opplysningstype = Opplysningstype.UKJENT,
    )

    data class Sivilstandendring(
        val sivilstand: String? = null,
        val bekreftelsesdato: LocalDate? = null,
        val gyldigFraOgMedDato: LocalDate? = null,
    )

    data class Adresseendring(
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
