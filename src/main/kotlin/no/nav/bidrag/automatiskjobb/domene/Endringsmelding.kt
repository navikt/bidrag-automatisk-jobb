package no.nav.bidrag.automatiskjobb.domene

data class Endringsmelding(
    val aktørid: String,
    val personidenter: Set<String>,
)
