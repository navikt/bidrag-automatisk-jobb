package no.nav.bidrag.aldersjustering.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.aldersjustering.persistence.entity.Barn
import no.nav.bidrag.aldersjustering.service.AldersjusteringService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class AldersjusteringController(
    private val aldersjusteringService: AldersjusteringService,
) {
    @GetMapping("/aldersjustering")
    @Operation(
        summary = "Hent barn som skal aldersjusteres",
        description = "Operasjon for å hente barn som skal aldersjusteres (som fyller 6, 11 eller 15 inneværende år) for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer map over barn som fyller 6, 11 eller 15 inneværende år.",
            ),
        ],
    )
    fun hentBarnSomSkalAldersjusteres(): Map<Int, List<Barn>> = aldersjusteringService.hentAlleBarnSomSkalAldersjusteresForÅr(2021)
}
