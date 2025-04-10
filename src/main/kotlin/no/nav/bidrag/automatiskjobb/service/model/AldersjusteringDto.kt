package no.nav.bidrag.automatiskjobb.service.model

import net.minidev.json.annotate.JsonIgnore
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto

data class AldersjusteringResponse(
    val aldersjustert: AldersjusteringResultatResponse,
    val ikkeAldersjustert: AldersjusteringResultatResponse,
)

data class AldersjusteringResultatResponse(
    val antall: Int,
    val stønadsider: List<Stønadsid>,
    val detaljer: List<AldersjusteringResultat>,
)

data class AldersjusteringAldersjustertResultat(
    val vedtaksid: Int,
    val stønadsid: Stønadsid,
    val vedtak: OpprettVedtakRequestDto,
) : AldersjusteringResultat(true)

data class AldersjusteringIkkeAldersjustertResultat(
    val stønadsid: Stønadsid,
    val begrunnelse: String,
) : AldersjusteringResultat(false)

abstract class AldersjusteringResultat(
    @JsonIgnore
    val aldersjustert: Boolean,
)

data class OpprettVedtakConflictResponse(
    val vedtaksid: Int,
)
