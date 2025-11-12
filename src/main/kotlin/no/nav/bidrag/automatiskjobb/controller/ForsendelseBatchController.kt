package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.forsendelse.distribuer.DistribuerForsendelseBidragBatch
import no.nav.bidrag.automatiskjobb.batch.forsendelse.opprett.OpprettForsendelseBatch
import no.nav.bidrag.automatiskjobb.batch.forsendelse.slett.SlettForsendelseSomSkalSlettesBidragBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Forsendelse batch")
class ForsendelseBatchController(
    private val slettForsendelseSomSkalSlettesBidragBatch: SlettForsendelseSomSkalSlettesBidragBatch,
    private val opprettForsendelseBatch: OpprettForsendelseBatch,
    private val distribuerForsendelseBidragBatch: DistribuerForsendelseBidragBatch,
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
        slettForsendelseSomSkalSlettesBidragBatch.start()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/batch/forsendelse/opprett")
    @Operation(
        summary = "Start kjøring av batch for å opprette forsendelse for aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som oppretter forsendelse for aldersjusteringer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for oppretting av forsendelser for aldersjusteringer ble startet.",
            ),
        ],
    )
    fun startOpprettForsendelseAldersjusteringBidragBatch(
        @RequestParam prosesserFeilet: Boolean = false,
    ): ResponseEntity<Any> {
        opprettForsendelseBatch.start(prosesserFeilet)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/batch/forsendelse/distribuer")
    @Operation(
        summary = "Start kjøring av batch for å distribuere forsendelse for aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som distribuerer forsendelser for aldersjusteringer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for distrubisjon av forsendelser for aldersjusteringer ble startet.",
            ),
        ],
    )
    fun startDistribuerForsendelseAldersjusteringBidragBatch(): ResponseEntity<Any> {
        distribuerForsendelseBidragBatch.start()
        return ResponseEntity.ok().build()
    }
}
