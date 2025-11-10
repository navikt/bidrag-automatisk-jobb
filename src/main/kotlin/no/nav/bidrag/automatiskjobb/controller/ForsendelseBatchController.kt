package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.forsendelse.slett.SlettForsendelseSomSkalSlettesBidragBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Forsendelse batch")
class ForsendelseBatchController(
    private val slettForsendelseSomSkalSlettesBidragBatch: SlettForsendelseSomSkalSlettesBidragBatch,
) {
    @PostMapping("/batch/forsendelse/slett")
    @Operation(
        summary = "Start kjøring av batch som sletter alle forsendelser som har blitt satt til skalSlettes=true.",
        description =
            "Operasjon for å starte kjøring av batch som skal slette alle forsendelser " +
                "som er satt til skal slettes med kolonnen skal_slettes=true",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for sletting av forsendelser ble startet",
            ),
        ],
    )
    fun starSlettForsendelserSomSkalSlettesBatch(): ResponseEntity<Any> {
        slettForsendelseSomSkalSlettesBidragBatch.startSlettForsendelserSomSkalSlettesBatch()
        return ResponseEntity.ok().build()
    }
}
