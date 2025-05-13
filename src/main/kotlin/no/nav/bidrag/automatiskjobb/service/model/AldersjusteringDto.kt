package no.nav.bidrag.automatiskjobb.service.model

import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultat

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
