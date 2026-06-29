package no.nav.bidrag.automatiskjobb.service.model

import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultat
import java.math.BigDecimal

data class AldersjusteringResponse(
    val aldersjustert: AldersjusteringResultatResponse,
    val ikkeAldersjustert: AldersjusteringResultatResponse,
)

data class AldersjusteringResultatResponse(
    val antall: Int,
    val stønadsider: List<Stønadsid>,
    val detaljer: List<AldersjusteringResultat>,
)

data class OpprettVedtakConflictResponse(
    val vedtaksid: Int,
)

data class SamværsklasseEndring(
    val periode: ÅrMånedsperiode,
    val gammelKlasse: Samværsklasse?,
    val nyKlasse: Samværsklasse?,
)

data class UnderholdskostnadEndring(
    val periode: ÅrMånedsperiode,
    val gammelNettoTilsynsutgift: BigDecimal?,
    val nyNettoTilsynsutgift: BigDecimal?,
    val gammelBarnetilsynMedStønad: BigDecimal?,
    val nyBarnetilsynMedStønad: BigDecimal?,
)

data class GrunnlagAvvikResultat(
    val aldersjusteringId: Int,
    val saksnummer: String,
    val kravhaver: String,
    val samværsklasseEndringer: List<SamværsklasseEndring>,
    val underholdskostnadEndringer: List<UnderholdskostnadEndring>,
)

data class VerifiserAldersjusteringerResultat(
    val totalt: Int,
    val antallMedAvvik: Int,
    val antallFeilet: Int,
    val avvik: List<GrunnlagAvvikResultat>,
)
