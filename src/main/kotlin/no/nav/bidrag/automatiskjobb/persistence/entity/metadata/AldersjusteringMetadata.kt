package no.nav.bidrag.automatiskjobb.persistence.entity.metadata

import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import java.math.BigDecimal

data class AldersjusteringMetadata(
    val beregningAvvik: BeregningAvvikMetadata? = null,
)

data class BeregningAvvikMetadata(
    val år: Int,
    val gammelBeløp: BigDecimal? = null,
    val nyttBeløp: BigDecimal? = null,
    val sjekket: Boolean = false,
    val saksnummer: String,
    val samværsklasseEndring: SamværsklasseEndringMetadata? = null,
    val underholdskostnadEndring: UnderholdskostnadEndringMetadata? = null,
)

data class SamværsklasseEndringMetadata(
    val gammelKlasse: Samværsklasse?,
    val nyKlasse: Samværsklasse?,
)

data class UnderholdskostnadEndringMetadata(
    val gammelNettoTilsynsutgift: BigDecimal?,
    val nyNettoTilsynsutgift: BigDecimal?,
    val gammelBarnetilsynMedStønad: BigDecimal?,
    val nyBarnetilsynMedStønad: BigDecimal?,
)
