package no.nav.bidrag.automatiskjobb.domene

import java.time.YearMonth

data class BarnetrygdBisysMelding(
    val søker: String,
    val barn: List<BarnEndretOpplysning>,
)

data class BarnEndretOpplysning(
    val ident: String,
    val årsakskode: BarnetrygdEndretType,
    val fom: YearMonth,
)

enum class BarnetrygdEndretType {
    RO, // Revurdering og Opphør
    RR, // Revurdering og Reduksjon
}
