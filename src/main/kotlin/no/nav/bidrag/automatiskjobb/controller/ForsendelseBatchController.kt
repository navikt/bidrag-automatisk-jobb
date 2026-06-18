package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.distribuer.DistribuerForsendelseBidragBatch
import no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.opprett.OpprettForsendelseBatch
import no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.slett.SlettForsendelseSomSkalSlettesBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Forsendelse batch")
class ForsendelseBatchController(
    private val slettForsendelseSomSkalSlettesBatch: SlettForsendelseSomSkalSlettesBatch,
    private val opprettForsendelseBatch: OpprettForsendelseBatch,
    private val distribuerForsendelseBidragBatch: DistribuerForsendelseBidragBatch,
) {
    @PostMapping("/batch/forsendelse/slett")
    @Operation(
        summary = "Start kjøring av batch som sletter forsendelser med skal_slettes=true, eller en spesifikk bestilling etter ID.",
        description =
            "Operasjon for å starte kjøring av batch som skal slette alle forsendelser " +
                "som er satt til skal slettes med kolonnen skal_slettes=true. " +
                "Dersom bestillingIds er angitt, slettes kun forsendelsene for de angitte bestillingene.",
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
    fun starSlettForsendelserSomSkalSlettesBatch(
        @RequestParam bestillingIds: String? = null,
    ): ResponseEntity<Any> {
        slettForsendelseSomSkalSlettesBatch.start(bestillingIds)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/batch/forsendelse/opprett")
    @Operation(
        summary = "Start kjøring av batch for å opprette forsendelse for aldersjusteringer.",
        description =
            "Operasjon for å starte kjøring av batch som oppretter forsendelse for aldersjusteringer. " +
                "Dersom bestillingIds er angitt, slettes og gjennopprettes forsendelsene for de angitte bestillingene.",
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
        @RequestParam bestillingIds: String? = null,
    ): ResponseEntity<Any> {
        opprettForsendelseBatch.start(prosesserFeilet, bestillingIds)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/batch/forsendelse/distribuer")
    @Operation(
        summary = "Start kjøring av batch for å distribuere forsendelse for aldersjusteringer.",
        description =
            "Operasjon for å starte kjøring av batch som distribuerer forsendelser for aldersjusteringer. " +
                "Dersom bestillingIds er angitt, distribueres kun forsendelsene for de angitte bestillingene.",
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
    fun startDistribuerForsendelseAldersjusteringBidragBatch(
        @RequestParam bestillingIds: String? = null,
    ): ResponseEntity<Any> {
        distribuerForsendelseBidragBatch.start(bestillingIds)
        return ResponseEntity.ok().build()
    }
}
