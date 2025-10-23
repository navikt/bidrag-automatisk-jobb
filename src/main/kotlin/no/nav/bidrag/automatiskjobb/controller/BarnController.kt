package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.generelt.oppdaterbarn.OppdaterBarnBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Barn")
class BarnController(
    private val oppdaterBarnBatch: OppdaterBarnBatch,
) {
    @PostMapping("/barn/oppdater")
    @Operation(
        summary = "Start kjøring av batch for å oppdatere barn med perioder for forskudd og bidrag",
        description =
            "Operasjon for å starte kjøring av batch som oppdaterer " +
                "barn med perioder for forskudd og bidrag ved å hente data fra bidrag-beløpshistorikk",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for oppdatering av barn perioder startet",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(
                name = "simuler",
                example = "true",
                description = "Simuleringsmodus. Default er true.",
                required = false,
            ),
            Parameter(
                name = "barn",
                example = "1,2,3",
                description =
                    "Liste over barn som det skal oppdateres for",
                required = false,
            ),
        ],
    )
    fun startOppdaterBarnBatch(
        @RequestParam simuler: Boolean = true,
        @RequestParam barn: String? = null,
        @RequestParam dagerTilbake: Int? = null,
    ): ResponseEntity<Any> {
        oppdaterBarnBatch.startOppdaterBarnBatch(barn, dagerTilbake ?: 1, simuler)
        return ResponseEntity.ok().build()
    }
}
