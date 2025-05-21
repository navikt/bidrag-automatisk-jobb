package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultatlisteResponse
import no.nav.bidrag.transport.automatiskjobb.HentAldersjusteringStatusRequest
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class AldersjusteringController(
    private val aldersjusteringService: AldersjusteringService,
    private val identUtils: IdentUtils,
) {
    @PostMapping("/aldersjustering/bidrag/status")
    @Operation(
        summary = "Hent status for aldersjustering",
        description =
            "Henter status for aldersjustering for barn og sak. Brukes i Bisys for å hente status for aldersjustering.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentAldersjusteringstatusForBarnOgSak(
        @RequestBody request: HentAldersjusteringStatusRequest,
    ): AldersjusteringResultatlisteResponse = aldersjusteringService.hentAldersjusteringstatusForBarnOgSak(request)

    @PostMapping("/test")
    @Operation(
        summary = "Hent status for aldersjustering",
        description =
            "Henter status for aldersjustering for barn og sak. Brukes i Bisys for å hente status for aldersjustering.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun test(
        @RequestBody request: Personident,
    ) = identUtils.hentAlleIdenter(request)
}
