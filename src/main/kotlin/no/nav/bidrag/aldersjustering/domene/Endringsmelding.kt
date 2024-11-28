package no.nav.bidrag.aldersjustering.domene

data class Endringsmelding(
    val aktørid: String,
    val personidenter: Set<String>,
)
