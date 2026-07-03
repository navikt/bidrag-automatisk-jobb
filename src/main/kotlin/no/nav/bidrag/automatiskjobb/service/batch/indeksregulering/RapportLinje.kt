package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import java.math.BigDecimal

data class RapportLinje(
    val saksnummer: String,
    val fnrBp: String,
    val fnrBa: String,
    val beløp: BigDecimal,
    val landkode: String? = null,
)
